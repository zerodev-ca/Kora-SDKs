import threading
from typing import Callable

class Heartbeat:
    def __init__(self, interval_sec: float, ping: Callable[[], None]):
        self.interval_sec = interval_sec
        self.ping = ping
        self.stop_event = threading.Event()
        self.thread = None

    def start(self) -> None:
        if self.thread is not None and self.thread.is_alive():
            return
        self.stop_event.clear()
        self.thread = threading.Thread(target=self._run, daemon=True)
        self.thread.start()

    def _run(self) -> None:
        while not self.stop_event.wait(self.interval_sec):
            try:
                self.ping()
            except Exception:
                pass

    def stop(self) -> None:
        if self.thread is None:
            return
        self.stop_event.set()
        self.thread = None
