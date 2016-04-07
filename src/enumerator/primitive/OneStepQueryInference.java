package enumerator.primitive;

import enumerator.context.EnumContext;
import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.tables.EnumFilterNamed;
import enumerator.primitive.tables.EnumJoinTableNodes;
import enumerator.primitive.tables.EnumProjection;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by clwang on 4/7/16.
 */
public class OneStepQueryInference {

    public static List<TableNode> infer(List<TableNode> inputTableNodes, Table output, EnumContext ec) {
        if (inputTableNodes.size() == 1) {
            List<TableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);

            tns.addAll(EnumAggrTableNode.enumAggregationNode(ec).stream()
                    .filter(tn -> {
                        try {
                            return tn.eval(new Environment()).equals(output);
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }).collect(Collectors.toList()));

            tns.addAll(EnumFilterNamed.enumFilterNamed(ec).stream().filter(tn -> {
                try {
                    return (tn.eval(new Environment()).equals(output));
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));
            tns.addAll(EnumProjection.enumProjection(ec, output));

            try {
                if (ec.getInputs().contains(inputTableNodes.get(0).eval(new Environment()))) {
                    tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec).stream().filter(tn -> {
                        try {
                            return (tn.eval(new Environment()).equals(output));
                        } catch (SQLEvalException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }).collect(Collectors.toList()));
                    tns.addAll(EnumProjection.enumProjection(ec, output));
                }
            } catch (SQLEvalException e) {
                e.printStackTrace();
            }

            return tns;
        } else if (inputTableNodes.size() == 2) {
            List<TableNode> tns = new ArrayList<>();
            ec.setTableNodes(inputTableNodes);
            tns.addAll(EnumJoinTableNodes.enumJoinWithFilter(ec).stream().filter(t -> {
                try {
                    return t.eval(new Environment()).equals(output);
                } catch (SQLEvalException e) {
                    e.printStackTrace();
                }
                return false;
            }).collect(Collectors.toList()));
            return tns;
        }
        return new ArrayList<>();
    }
}
