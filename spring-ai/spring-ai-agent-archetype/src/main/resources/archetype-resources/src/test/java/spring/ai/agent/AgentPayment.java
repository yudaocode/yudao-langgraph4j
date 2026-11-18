#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public interface AgentPayment {

    class Tools {

        @com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY, getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
        @dev.langchain4j.internal.compat.Java8Record({ "product", "code" })
        final static class Transaction extends dev.langchain4j.internal.compat.Java8RecordBase {

            @JsonPropertyDescription("the product name bought")
            private final String product;

            @JsonPropertyDescription("the product name bought")
            public String product() {
                return this.product;
            }

            @JsonPropertyDescription("code operation")
            private final String code;

            @JsonPropertyDescription("code operation")
            public String code() {
                return this.code;
            }

            @com.fasterxml.jackson.annotation.JsonCreator()
            Transaction(@JsonPropertyDescription("the product name bought") @com.fasterxml.jackson.annotation.JsonProperty("product") String product, @JsonPropertyDescription("code operation") @com.fasterxml.jackson.annotation.JsonProperty("code") String code) {
                this.product = product;
                this.code = code;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other)
                    return true;
                if (other == null || getClass() != other.getClass())
                    return false;
                Transaction that = (Transaction) other;
                return java.util.Objects.equals(this.product, that.product) && java.util.Objects.equals(this.code, that.code);
            }

            @Override
            public int hashCode() {
                int result = 0;
                result = 31 * result + java.util.Objects.hashCode(this.product);
                result = 31 * result + java.util.Objects.hashCode(this.code);
                return result;
            }

            @Override
            public String toString() {
                return "Transaction[product=" + java.lang.String.valueOf((Object) this.product) + ", code=" + java.lang.String.valueOf((Object) this.code) + "]";
            }
        }

        @Tool(name = "submit-payment", description = "submit a payment for purchasing a specific product")
        Transaction submitPayment(@ToolParam(description = "the product name") String product, @ToolParam(description = "the product price") double price, @ToolParam(description = "the product currency") String currency, @ToolParam(description = "International Bank Account Number (IBAN)") String iban, ToolContext toolContext) {
            return new Transaction(product, "123456789B");
        }

        @Tool(name = "retrieve-iban", description = "retrieve IBAN information")
        String retrieveIBAN() {
            return """
                GB82WEST12345698765432
                """;
        }
    }

    static Tools tools() {
        return new Tools();
    }
}
