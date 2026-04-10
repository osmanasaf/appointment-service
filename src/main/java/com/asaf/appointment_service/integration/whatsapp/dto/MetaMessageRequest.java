package com.asaf.appointment_service.integration.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetaMessageRequest(
    @JsonProperty("messaging_product") String messagingProduct,
    @JsonProperty("recipient_type") String recipientType,
    String to,
    String type,
    Template template,
    Text text,
    Interactive interactive
) {
    public static MetaMessageRequest template(String to, String templateName, String languageCode, List<Component> components) {
        return new MetaMessageRequest(
            "whatsapp",
            "individual",
            to,
            "template",
            new Template(templateName, new Language(languageCode), components),
            null,
            null
        );
    }

    public static MetaMessageRequest text(String to, String body, boolean previewUrl) {
        return new MetaMessageRequest(
            "whatsapp",
            "individual",
            to,
            "text",
            null,
            new Text(body, previewUrl),
            null
        );
    }

    public static MetaMessageRequest interactiveButtons(String to, String headerText, String bodyText, String footerText, List<Button> buttons) {
        return new MetaMessageRequest(
            "whatsapp",
            "individual",
            to,
            "interactive",
            null,
            null,
            new Interactive(
                "button",
                headerText != null ? new InteractiveHeader("text", headerText) : null,
                new InteractiveBody(bodyText),
                footerText != null ? new InteractiveFooter(footerText) : null,
                new InteractiveAction(buttons, null)
            )
        );
    }

    public static MetaMessageRequest interactiveList(String to, String headerText, String bodyText, String footerText, String buttonText, List<Section> sections) {
        return new MetaMessageRequest(
            "whatsapp",
            "individual",
            to,
            "interactive",
            null,
            null,
            new Interactive(
                "list",
                headerText != null ? new InteractiveHeader("text", headerText) : null,
                new InteractiveBody(bodyText),
                footerText != null ? new InteractiveFooter(footerText) : null,
                new InteractiveAction(null, new ListAction(buttonText, sections))
            )
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Template(
        String name,
        Language language,
        List<Component> components
    ) {}

    public record Language(String code) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Component(
        String type,
        List<Parameter> parameters
    ) {
        public static Component body(List<Parameter> parameters) {
            return new Component("body", parameters);
        }

        public static Component header(List<Parameter> parameters) {
            return new Component("header", parameters);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Parameter(
        String type,
        String text
    ) {
        public static Parameter text(String value) {
            return new Parameter("text", value);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Text(
        String body,
        @JsonProperty("preview_url") boolean previewUrl
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Interactive(
        String type,
        InteractiveHeader header,
        InteractiveBody body,
        InteractiveFooter footer,
        InteractiveAction action
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractiveHeader(
        String type,
        String text
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractiveBody(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractiveFooter(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractiveAction(
        List<Button> buttons,
        ListAction listAction
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Button(
        String type,
        Reply reply
    ) {
        public static Button reply(String id, String title) {
            return new Button("reply", new Reply(id, title));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Reply(
        String id,
        String title
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ListAction(
        String button,
        List<Section> sections
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Section(
        String title,
        List<Row> rows
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Row(
        String id,
        String title,
        String description
    ) {}
}
