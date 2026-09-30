import uuid
import platform
import hashlib

class Machine:
    def mac(self) -> str:
        mac_int = uuid.getnode()
        return ":".join(f"{(mac_int >> ele) & 0xff:02x}" for ele in range(40, -1, -8))

    def hwid(self) -> str:
        h = hashlib.sha256()
        h.update(platform.system().encode("utf-8"))
        h.update(platform.machine().encode("utf-8"))
        h.update(platform.node().encode("utf-8"))
        h.update(self.mac().encode("utf-8"))
        return h.hexdigest()
