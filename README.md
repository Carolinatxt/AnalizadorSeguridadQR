# AnalizadorSeguridadQR
Aplicacion Android para escanear y analizar la seguridad de URLs obtenidas desde codigos QR.

## Nota de seguridad (rate limiting en produccion)
El backend usa `get_remote_address` para limitar solicitudes por IP.
Detras de proxy inverso (Render/Koyeb/Nginx), varios clientes pueden compartir IP y perder precision.

Para produccion:
- Confiar solo en cabeceras de forwarding provenientes de proxies verificados.
- No aceptar `X-Forwarded-For` de origen no confiable.
