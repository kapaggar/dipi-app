#!/usr/bin/env python3
"""Create the one constant Android provisioning code. Never prints secret material."""
import argparse
import base64
import getpass
import hashlib
import os
from pathlib import Path
import re


def provisioning_code(secret: str, secret_iv: str) -> str:
    if not secret or not secret_iv:
        raise ValueError("Both existing encryption values are required")
    material = (hashlib.sha256(secret.encode()).hexdigest()[:32]
                + hashlib.sha256(secret_iv.encode()).hexdigest()[:16]).encode("ascii")
    payload = base64.urlsafe_b64encode(material).decode("ascii").rstrip("=")
    return "DIPI-WA1." + payload + "." + hashlib.sha256(material).hexdigest()[:8]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="New owner-only file outside this repository")
    parser.add_argument("--legacy-php", type=Path, help="Read the existing simple_crypt secret_key/secret_iv constants without executing PHP")
    args = parser.parse_args()
    target = args.output.expanduser().resolve()
    root = Path(__file__).resolve().parents[1]
    if target == root or root in target.parents:
        parser.error("The provisioning code must be stored outside the repository")
    if args.legacy_php:
        source = args.legacy_php.read_text()
        offset = source.find("function simple_crypt")
        if offset < 0:
            parser.error("No simple_crypt function was found")
        source = source[offset:]
        values = []
        for name in ("secret_key", "secret_iv"):
            match = re.search(r"\$" + name + r'''\s*=\s*(['"])(.*?)\1''', source)
            if not match:
                parser.error("Existing encryption constants could not be identified")
            values.append(match.group(2))
    else:
        values = [getpass.getpass("Existing secret key: "), getpass.getpass("Existing secret IV: ")]
    code = provisioning_code(*values)
    fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as output:
        output.write(code + "\n")
    print("Provisioning code written to the requested owner-only file. No email was sent.")


if __name__ == "__main__":
    main()
