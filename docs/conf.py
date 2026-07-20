project = "InterPro MCP Server"
author = "Matthias Blum"
copyright = "2026, EMBL-EBI"
release = "0.1.0"

extensions = [
    "myst_parser",
]

source_suffix = {".md": "markdown"}
root_doc = "index"

myst_enable_extensions = [
    "colon_fence",   # ::: fences, e.g. for admonitions
    "deflist",
    "linkify",       # bare URLs become links
]
myst_heading_anchors = 3

exclude_patterns = ["_build", "Thumbs.db", ".DS_Store"]

html_theme = "furo"
html_title = "InterPro MCP Server"
