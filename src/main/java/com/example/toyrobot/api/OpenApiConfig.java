package com.example.toyrobot.api;

import com.example.toyrobot.constant.ApiDocs;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI toyRobotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(ApiDocs.Info.TITLE)
                        .version(ApiDocs.Info.VERSION)
                        .description(ApiDocs.Info.DESCRIPTION))
                // Fixed rather than inferred from each request, so the exported openapi.yaml is reproducible.
                .servers(List.of(new Server().url(ApiDocs.Info.SERVER_URL).description(ApiDocs.Info.SERVER_DESCRIPTION)))
                .tags(List.of(
                        new Tag().name(ApiDocs.Tags.ROBOTS).description(ApiDocs.Tags.ROBOTS_DESCRIPTION),
                        new Tag().name(ApiDocs.Tags.CONTROL).description(ApiDocs.Tags.CONTROL_DESCRIPTION)));
    }
}
