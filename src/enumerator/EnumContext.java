package enumerator;

import sql.lang.DataType.ValType;
import sql.lang.DataType.Value;
import sql.lang.Table;
import sql.lang.ast.table.AggregationNode;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An environment used to store
 * The context is a data structure
 *  used to store constraint contexts
 * Created by clwang on 12/23/15.
 */
public class EnumContext {

    private int maxFilterLength = 2;

    private List<TableNode> tablesNodes = new ArrayList<>();
    private List<ValNode> valNodes = new ArrayList<>();

    // ** the only part of the context that should not mutate once intialized
    private List<TableNode> parameterizedTables = new ArrayList<>();

    List<Function<List<Value>, Value>> aggrfuns = new ArrayList<>();

    private Map<String, ValType> typeMap = new HashMap<>();

    public EnumContext() {

    }

    public void setTableNodes(List<TableNode> tns) {
        this.tablesNodes = tns;
    }

    public void setValNodes(List<ValNode> vns) { this.valNodes = vns; }

    public void setMaxFilterLength(int maxLength) { this.maxFilterLength = maxLength; }
    public int getMaxFilterLength() { return this.maxFilterLength; }

    public void setParameterizedTables(List<TableNode> tbs) { this.parameterizedTables = tbs; }

    public EnumContext(List<Table> tbs, Constraint c) {
        tablesNodes = tbs.stream().map(t -> new NamedTable(t)).collect(Collectors.toList());
        valNodes = c.constValNodes();
        this.aggrfuns = c.getAggrFuns();
    }

    // Extend atomic tables in the context
    public static EnumContext extendTable(
            EnumContext ec, List<TableNode> tables) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        newEC.tablesNodes.addAll(tables);
        return newEC;
    }

    // Extend the atomic values in the context
    public static EnumContext extendTypeMap(
            EnumContext ec, Map<String, ValType> map) {
        EnumContext newEC = EnumContext.deepCopy(ec);
        for (Map.Entry<String, ValType> i : map.entrySet()) {
            newEC.valNodes.add(new NamedVal(i.getKey()));
        }
        newEC.typeMap.putAll(ec.typeMap);
        newEC.typeMap.putAll(map);
        return newEC;
    }

    public List<TableNode> getTableNodes() {
        return this.tablesNodes;
    }

    public List<ValNode> getValNodes() {
        List<ValNode> allValNodes = new ArrayList<ValNode>();
        allValNodes.addAll(this.valNodes);
        return allValNodes;
    }

    // get aggregation functions that
    public List<Function<List<Value>, Value>> getAggrFuns(ValType type) {
        List<Function<List<Value>, Value>> fs = AggregationNode.getAllAggrFunctions(type);
        return fs.stream().filter(f -> this.aggrfuns.contains(f)).collect(Collectors.toList());
    }

    public ValType getValType(String name) {
        return typeMap.get(name);
    }

    public static EnumContext deepCopy(EnumContext ec) {
        EnumContext newEC = new EnumContext();
        newEC.tablesNodes.addAll(ec.tablesNodes);
        newEC.aggrfuns.addAll(ec.aggrfuns);
        newEC.valNodes.addAll(ec.valNodes);
        newEC.typeMap.putAll(ec.typeMap);
        newEC.maxFilterLength = ec.maxFilterLength;
        newEC.parameterizedTables = ec.parameterizedTables;
        return newEC;
    }
}
