package sql.lang.ast.table;

import enumerator.parameterized.InstantiateEnv;
import sql.lang.DataType.ValType;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.Hole;
import sql.lang.exception.SQLEvalException;
import util.IndentionManagement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by clwang on 12/21/15.
 */
public class RenameTableNode implements TableNode {

    String newTableName;
    List<String> newFieldNames;
    TableNode tableNode;

    boolean renameTable = false;
    boolean renameFields = false;

    private RenameTableNode(String ntn, List<String> nfn, TableNode tn, boolean rt, boolean rf) {
        this.newFieldNames = nfn;
        this.newTableName = ntn;
        this.renameFields = rf;
        this.tableNode = tn;
        this.renameTable = rt;
    }

    public RenameTableNode(List<String> nfn, TableNode tn) {
        this.newFieldNames = nfn;
        this.tableNode = tn;
        this.renameFields = true;
        this.newTableName = tn.getTableName();
    }

    public RenameTableNode(String tableName, List<String> fieldsName, TableNode tn) {
        this.newTableName = tableName;
        this.newFieldNames = fieldsName;
        this.tableNode = tn;
        renameTable = true;
        renameFields = true;
    }

    public RenameTableNode(String newTableName, TableNode tn) {
        this.newTableName = newTableName;
        this.newFieldNames = tn.getSchema();
        this.tableNode = tn;
        renameTable = true;
    }

    @Override
    public Table eval(Environment env) throws SQLEvalException {
        Table table = tableNode.eval(env);
        if (this.renameTable == true) {
            table.updateName(this.newTableName);
        }
        if (this.renameFields == true) {
            table.updateMetadata(this.newFieldNames);
        }
        return table;
    }

    @Override
    public String getTableName() {
        return this.newTableName;
    }

    @Override
    public List<ValType> getSchemaType() {
        return tableNode.getSchemaType();
    }

    @Override
    public int getNestedQueryLevel() {
        return tableNode.getNestedQueryLevel();
    }

    @Override
    public String prettyPrint(int indentLv) {
        String result = "(" + tableNode.prettyPrint(1).trim() + ") AS ";
        String newSchema = this.newTableName;

        String fieldString = "[";
        boolean flag = true;
        for (String s : this.newFieldNames) {
            if(flag == true) {
                fieldString += s;
                flag = false;
            } else
                fieldString += ", " + s;
        }
        fieldString += "]";

        if (this.renameFields == true)
            newSchema += fieldString;

        result = result + newSchema;
        return IndentionManagement.addIndention(result, indentLv);
    }

    @Override
    public List<Hole> getAllHoles() {
        return tableNode.getAllHoles();
    }

    @Override
    public TableNode instantiate(InstantiateEnv env) {
        return new RenameTableNode(
                this.newTableName,
                this.newFieldNames,
                this.tableNode.instantiate(env),
                this.renameTable,
                this.renameFields);
    }

    @Override
    public List<String> getSchema() {
        if (this.newTableName.equals("anonymous"))
            return this.newFieldNames;
        else {
            // add the qualifier
            return this.newFieldNames.stream()
                    .map(s -> this.newTableName + "." + s).collect(Collectors.toList());
        }
    }
}
