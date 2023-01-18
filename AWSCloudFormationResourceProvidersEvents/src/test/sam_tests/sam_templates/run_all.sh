mvn clean package -DskipTests && \
 cfn invoke -v resource CREATE ./src/test/sam_tests/sam_templates/create_rule.json && \
 cfn invoke -v resource READ ./src/test/sam_tests/sam_templates/read_rule.json && \
 cfn invoke -v resource UPDATE ./src/test/sam_tests/sam_templates/update_rule.json && \
 cfn invoke -v resource LIST ./src/test/sam_tests/sam_templates/list_rules.json && \
 cfn invoke -v resource DELETE ./src/test/sam_tests/sam_templates/delete_rule.json
