#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public interface AgentMarketplace {

    class Tools {

        @com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY, getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
        @dev.langchain4j.internal.compat.Java8Record({ "name", "price", "currency" })
        final static class Product extends dev.langchain4j.internal.compat.Java8RecordBase {

            @JsonPropertyDescription("the product name")
            private final String name;

            @JsonPropertyDescription("the product name")
            public String name() {
                return this.name;
            }

            @JsonPropertyDescription("the product price")
            private final double price;

            @JsonPropertyDescription("the product price")
            public double price() {
                return this.price;
            }

            @JsonPropertyDescription("the product price currency")
            private final String currency;

            @JsonPropertyDescription("the product price currency")
            public String currency() {
                return this.currency;
            }

            @com.fasterxml.jackson.annotation.JsonCreator()
            Product(@JsonPropertyDescription("the product name") @com.fasterxml.jackson.annotation.JsonProperty("name") String name, @JsonPropertyDescription("the product price") @com.fasterxml.jackson.annotation.JsonProperty("price") double price, @JsonPropertyDescription("the product price currency") @com.fasterxml.jackson.annotation.JsonProperty("currency") String currency) {
                this.name = name;
                this.price = price;
                this.currency = currency;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other)
                    return true;
                if (other == null || getClass() != other.getClass())
                    return false;
                Product that = (Product) other;
                return java.util.Objects.equals(this.name, that.name) && Double.compare(this.price, that.price) == 0 && java.util.Objects.equals(this.currency, that.currency);
            }

            @Override
            public int hashCode() {
                int result = 0;
                result = 31 * result + java.util.Objects.hashCode(this.name);
                result = 31 * result + Double.hashCode(this.price);
                result = 31 * result + java.util.Objects.hashCode(this.currency);
                return result;
            }

            @Override
            public String toString() {
                return "Product[name=" + java.lang.String.valueOf((Object) this.name) + ", price=" + java.lang.String.valueOf((Object) this.price) + ", currency=" + java.lang.String.valueOf((Object) this.currency) + "]";
            }
        }

        @Tool(description = "search for a specific product in the marketplace")
        Product searchByProduct(@ToolParam(description = "the product name to search") String product, ToolContext toolContext) {
            return new Product("X", 1000, "EUR");
        }
    }

    static Tools tools() {
        return new Tools();
    }
}
