package forward_enumeration.enumerative_search.components;

import forward_enumeration.context.EnumContext;
import forward_enumeration.container.QueryContainer;
import forward_enumeration.primitive.FilterEnumerator;
import sql.lang.datatype.ValType;
import sql.lang.ast.Environment;
import sql.lang.ast.filter.Filter;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.SelectNode;
import sql.lang.ast.table.TableNode;
import sql.lang.ast.val.NamedVal;
import sql.lang.ast.val.ValNode;
import sql.lang.exception.SQLEvalException;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enumerate filtered named tables from enum context
 * Created by clwang on 2/1/16.
 */
public class EnumFilterNamed {

    /**
     * Enumerate filter nodes for named tables: given a named table, we will generate filter for the named tables.
     * @return
     */
    public static List<TableNode> enumFilterNamed(EnumContext ec) {
        List<TableNode> targets = ec.getTableNodes().stream()
                .filter(tn -> (tn instanceof NamedTable))
                .collect(Collectors.toList());

        List<TableNode> result = new ArrayList<>();

        for (TableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            // we allow using exists when enumerating filters for a named table.
            boolean allowExists = true;
            List<Filter> filters = FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2, allowExists);

            for (Filter f : filters) {
                TableNode sn = new SelectNode(vals, tn, f);
                result.add(sn);
            }
        }
        return result;
    }

    // Emit enumerated query on the fly, whether to store them or not is determined by qc
    public static void emitEnumFilterNamed(EnumContext ec, QueryContainer qc) {

        List<TableNode> targets = ec.getTableNodes();

        for (TableNode tn : targets) {

            // the selection args are complete
            List<ValNode> vals = tn.getSchema().stream()
                    .map(s -> new NamedVal(s))
                    .collect(Collectors.toList());

            Map<String, ValType> typeMap = new HashMap<>();
            for (int i = 0; i < tn.getSchema().size(); i ++) {
                typeMap.put(tn.getSchema().get(i), tn.getSchemaType().get(i));
            }

            // enum filters
            EnumContext ec2 = EnumContext.extendValueBinding(ec, typeMap);

            boolean allowExists = true;
            List<Filter> filters = FilterEnumerator.enumFiltersLR(vals, ec2.getValNodes(), ec2, allowExists);

            for (Filter f : filters) {
                TableNode sn = new SelectNode(vals, tn, f);
                // when a table is generated, emit it to the query chest
                qc.insertQuery(RenameTNWrapper.tryRename(sn));
                // inserting an edge from eval(tn) --> eval(sn)

                if (qc.useTableLinks()) {
                    // if qc use filter links, we can put filter links into qc
                    try {
                        qc.getTableLinks().insertEdge(
                                qc.getRepresentative(tn.eval(new Environment())),
                                qc.getRepresentative(sn.eval(new Environment())));
                    } catch (SQLEvalException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
