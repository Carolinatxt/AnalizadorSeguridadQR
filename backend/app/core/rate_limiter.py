from slowapi import Limiter
from slowapi.util import get_remote_address

# V0: identificacion por IP remota simple. Si hay proxy inverso,
# debe revisarse para evitar agrupar usuarios bajo una misma IP.
limiter = Limiter(key_func=get_remote_address)
