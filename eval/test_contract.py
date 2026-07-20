"""Contract test (no LLM).

Call each MCP tool directly over JSON-RPC and assert on known-answer cases. This
checks the tools return the right data.

Run with `pytest -m contract`.
"""

import pytest

pytestmark = pytest.mark.contract

# id, tool, arguments, substrings expected in the response (matched case-insensitively)
CASES = [
    (
        "getEntry PF00069 -> Pfam, integrated IPR000719",
        "getEntry",
        {"accession": "PF00069"},
        ["protein kinase", "IPR000719"],
    ),
    (
        "getEntry PS50070 -> PROSITE profile (Kringle, IPR000001)",
        "getEntry",
        {"accession": "PS50070"},
        ["kringle", "IPR000001"],
    ),
    (
        "getProteinEntries P99999 -> Cytochrome c with matches",
        "getProteinEntries",
        {"uniprotAccession": "P99999"},
        ["cytochrome c", "matches"],
    ),
    (
        "getTaxonomicDistribution IPR000719 depth=1 -> superkingdom breakdown",
        "getTaxonomicDistribution",
        {"accession": "IPR000719", "depth": 1},
        ["Eukaryota", "Bacteria", "domain", "percent"],
    ),
    (
        "getTaxonomicDistribution PF00069 depth=2 minProteins -> kingdom level, pruned",
        "getTaxonomicDistribution",
        {"accession": "PF00069", "depth": 2, "minProteins": 50000},
        ["kingdom", "Metazoa"],
    ),
    (
        "searchByGoTerm GO:0004672 -> InterPro entries",
        "searchByGoTerm",
        {"goTerm": "GO:0004672"},
        ["IPR", "count"],
    ),
    (
        "getDomainArchitectures IPR000007 -> contains PF01167",
        "getDomainArchitectures",
        {"accession": "IPR000007", "pageSize": 3},
        ["PF01167", "unique_proteins"],
    ),
    (
        "searchDomainArchitectures Kringle+Trypsin ordered+exact -> single architecture",
        "searchDomainArchitectures",
        {"domains": "PF00051,PF00089", "ordered": True, "exact": True},
        ["PF00051:IPR000001-PF00089:IPR001254", "unique_proteins"],
    ),
    (
        "getEntryProteins IPR000001 reviewed+human -> Swiss-Prot proteins with locations",
        "getEntryProteins",
        {
            "accession": "IPR000001",
            "reviewedOnly": True,
            "taxId": "9606",
            "pageSize": 3,
        },
        ["Homo sapiens", "reviewed_only", "locations"],
    ),
    (
        "getEntryStructures PF00069 -> PDB structures with experiment/resolution",
        "getEntryStructures",
        {"accession": "PF00069", "pageSize": 3},
        ["experiment_type", "resolution", "structures"],
    ),
    # Validation-only cases (no network / no live job): confirm the tools' guards.
    (
        "matchSequences validates empty input",
        "matchSequences",
        {"sequencesOrMd5s": ""},
        ["Provide one protein sequence or MD5"],
    ),
    (
        "analyzeSequence validates empty input",
        "analyzeSequence",
        {"sequence": ""},
        ["Provide a protein sequence"],
    ),
    (
        "getSequenceAnalysis validates the jobId",
        "getSequenceAnalysis",
        {"jobId": "not-a-job"},
        ["valid InterProScan jobId"],
    ),
]


@pytest.mark.parametrize(
    "tool,arguments,expected",
    [pytest.param(tool, args, exp, id=label) for label, tool, args, exp in CASES],
)
def test_tool_contract(call, tool, arguments, expected):
    text = call(tool, arguments)
    lowered = text.lower()
    missing = [s for s in expected if s.lower() not in lowered]
    assert not missing, f"missing {missing}\n  got: {text[:300]}"
