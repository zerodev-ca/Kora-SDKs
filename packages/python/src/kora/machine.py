import uuid
import socket
import platform
import hashlib

class Machine:
    def mac(self) -> str:
        node = uuid.getnode()
        return ":".join(f"{(node >> shift) & 0xff:02x}" for shift in range(40, -1, -8))

    def hwid(self) -> str:
        digest = hashlib.sha256()
        for part in (platform.system(), platform.machine(), platform.node(), self.mac()):
            digest.update(part.encode("utf-8"))
        return digest.hexdigest()

    def name(self) -> str:
        return socket.gethostname()

    def platform(self) -> str:
        return f"{platform.system().lower()}-{platform.machine().lower()}"
