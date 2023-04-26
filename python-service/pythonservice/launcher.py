"""
Launcher
"""

import os

from aiohttp import web

PORT = os.getenv("PYTHON_SERVICE_PORT")

from consumer import consumerRoutes


if __name__ == "__main__":
    
    app = web.Application()
    app.add_routes(consumerRoutes)
    web.run_app(app, host="localhost", port=PORT)