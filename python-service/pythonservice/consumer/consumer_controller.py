from aiohttp import web
from aiohttp.web_request import Request

consumerRoutes = web.RouteTableDef()

@consumerRoutes.get("/api/consumer/verify")
async def handle_verify_consumer(request: Request):
    if not request.body_exists:
        return web.Response(status=400)

    data = await request.json()
    return web.json_response(status=200, data=dict(code=0))
