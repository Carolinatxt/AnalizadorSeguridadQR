from slowapi import Limiter
from slowapi.util import get_remote_address

# AVISO DE SEGURIDAD: get_remote_address usa la IP directa de la conexion TCP.
# En despliegue con proxy inverso (Nginx, Render, Koyeb), muchos clientes
# pueden compartir la IP del proxy y el limite pierde precision.
# Para produccion real, usar solo cabeceras de forwarding desde proxies
# verificados y de confianza (por ejemplo X-Forwarded-For controlada).
limiter = Limiter(key_func=get_remote_address)
