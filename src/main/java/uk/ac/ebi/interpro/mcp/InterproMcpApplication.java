package uk.ac.ebi.interpro.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import uk.ac.ebi.interpro.mcp.tools.AnalysisTool;
import uk.ac.ebi.interpro.mcp.tools.ArchitectureTool;
import uk.ac.ebi.interpro.mcp.tools.EntryProteinTool;
import uk.ac.ebi.interpro.mcp.tools.EntryTool;
import uk.ac.ebi.interpro.mcp.tools.ProteinTool;
import uk.ac.ebi.interpro.mcp.tools.SearchTool;
import uk.ac.ebi.interpro.mcp.tools.SequenceTool;
import uk.ac.ebi.interpro.mcp.tools.StructureTool;
import uk.ac.ebi.interpro.mcp.tools.TaxonomyTool;

@SpringBootApplication
public class InterproMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterproMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider interproTools(SearchTool searchTool,
                                              EntryTool entryTool,
                                              ProteinTool proteinTool,
                                              EntryProteinTool entryProteinTool,
                                              StructureTool structureTool,
                                              ArchitectureTool architectureTool,
                                              TaxonomyTool taxonomyTool,
                                              SequenceTool sequenceTool,
                                              AnalysisTool analysisTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchTool, entryTool, proteinTool, entryProteinTool, structureTool,
                        architectureTool, taxonomyTool, sequenceTool, analysisTool)
                .build();
    }
}
