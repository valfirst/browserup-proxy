import json
import base64
import typing
import tempfile

from time import sleep

import re

from datetime import datetime
from datetime import timezone

import falcon

from mitmproxy import ctx

from mitmproxy import version
from mitmproxy.utils import strutils
from mitmproxy.net.http import cookies
from mitmproxy import http

class LatencyResource:

    def addon_path(self):
        return "latency"

    def __init__(self, latency_addon):
        self.latency_addon = latency_addon

    def on_get(self, req, resp, method_name):
        getattr(self, "on_" + method_name)(req, resp)

    def on_set_latency(self, req, resp):
        self.latency_addon.latency_ms = int(req.get_param('latency'))


class LatencyAddOn:

    def __init__(self):
        self.num = 0
        self.latency_ms = 0

    def get_resource(self):
        return LatencyResource(self)

    def response(self, flow):
        if self.latency_ms != 0:
            sleep(self.latency_ms / 1000)

addons = [
    LatencyAddOn()
]
