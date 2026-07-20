"""Agentic eval (needs the LLM).

Drive the InterPro MCP server through the Claude CLI headlessly and check that
each natural-language prompt 
    (a) triggers the right tool and 
    (b) yields the expected answer. 
    
Needs `claude` on PATH and logged in; the whole layer is skipped
automatically when `claude` is absent. 

Run locally with `pytest -m agentic` (or `make eval`).

Answer assertions are intentionally loose. This layer checks tool selection and
that the model read the output coherently, not exact field values.
"""

import pytest

pytestmark = pytest.mark.agentic

# id, prompt, expected tool (substring of mcp__interpro__<tool>), expected answer substrings
CASES = [
    (
        "PF00069 -> getEntry",
        "What InterPro entry is PF00069?",
        "getEntry",
        ["protein kinase", "IPR000719"],
    ),
    (
        "P99999 -> getProteinEntries",
        "What domains are in UniProt protein P99999?",
        "getProteinEntries",
        ["cytochrome c"],
    ),
    (
        "IPR000719 taxonomy -> getTaxonomicDistribution",
        "Is the protein kinase domain IPR000719 mostly eukaryotic or bacterial?",
        "getTaxonomicDistribution",
        ["eukary"],
    ),
    (
        "GO:0004672 -> searchByGoTerm",
        "Which InterPro entries are annotated with GO:0004672?",
        "searchByGoTerm",
        ["IPR"],
    ),
    (
        "zinc finger -> searchInterPro",
        "Search InterPro for zinc finger domains.",
        "searchInterPro",
        ["zinc finger"],
    ),
    (
        "IPR000007 architectures -> getDomainArchitectures",
        "What domain architectures does IPR000007 appear in?",
        "getDomainArchitectures",
        ["architecture"],
    ),
    (
        "IPR000001 proteins -> getEntryProteins",
        "List some reviewed human proteins that contain the kringle domain IPR000001.",
        "getEntryProteins",
        ["kringle"],
    ),
    (
        "PF00069 structures -> getEntryStructures",
        "What PDB structures contain the protein kinase domain PF00069?",
        "getEntryStructures",
        ["structure"],
    ),
    (
        "PS50070 -> getEntry",
        "Tell me about PROSITE profile PS50070.",
        "getEntry",
        ["kringle"],
    ),
]


@pytest.mark.parametrize(
    "prompt,want_tool,want_subs",
    [pytest.param(prompt, tool, subs, id=label) for label, prompt, tool, subs in CASES],
)
def test_agentic(run_agent, prompt, want_tool, want_subs):
    tools, final, err = run_agent(prompt)
    tool_ok = any(want_tool in t for t in tools)
    missing = [s for s in want_subs if s.lower() not in final.lower()]
    detail = f"\n  tools={tools or '(none)'}\n  answer={final[:200]!r}"
    if not final and err:
        detail += f"\n  stderr={err.strip()[:200]}"
    assert tool_ok, f"expected a {want_tool} call{detail}"
    assert not missing, f"answer missing {missing}{detail}"
