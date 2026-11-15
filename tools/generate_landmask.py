import urllib.request
import json
import zlib
import base64

# Simple offline landmass polygons (continents bounding polygons / rasterization)
# We can create a 360 x 180 1-degree resolution binary bitmap or 180x90.
# A 360x180 bitmap is 360*180 = 64,800 bits = 8,100 bytes!
# That is ~8 KB uncompressed, and ~2-3 KB gzipped / base64!
# Let's generate a coarse world land mask using low-res polygon definitions.

# Or fetch Natural Earth 110m land if online, or approximate with simplified polygon shapes.
