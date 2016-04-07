package enumerator.tableenumerator;

import enumerator.context.EnumContext;
import enumerator.context.QueryChest;
import enumerator.context.TableTreeNode;
import enumerator.primitive.tables.EnumAggrTableNode;
import enumerator.primitive.tables.EnumFilterNamed;
import enumerator.primitive.tables.EnumJoinTableNodes;
import enumerator.primitive.tables.EnumProjection;
import sql.lang.Table;
import sql.lang.ast.Environment;
import sql.lang.ast.table.TableNode;
import sql.lang.exception.SQLEvalException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by clwang on 3/31/16.
 */
public class CanonicalTableEnumeratorOnTheFly extends AbstractTableEnumerator {

    @Override
    public QueryChest enumTable(EnumContext ec, int depth) {

        depth = 2;

        QueryChest qc = QueryChest.initWithInputTables(ec.getInputs());
        enumTableWithoutProj(ec, qc, depth); // all intermediate result are stored in qc

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumProjection.emitEnumProjection(ec, ec.getOutputTable(), qc);

        System.out.println("[Consistent Table number] " + qc.getEdges().getDirectLinkCount(ec.getOutputTable()));


        Set<Table> leafNodes = new HashSet<>(); leafNodes.addAll(ec.getInputs());
        List<TableTreeNode> trees = qc.getEdges().findTableTrees(ec.getOutputTable(), leafNodes, 4);

        int totalQueryCount = 0;
        for (TableTreeNode t : trees) {
            //System.out.println("--------------------------");
            t.inferQuery(ec);
            List<TableNode> tns = t.treeToQuery();
            System.out.println("Query corresponds to this one: " + tns.size());
            totalQueryCount += tns.size();

            //t.print(0);

            //for (TableNode tn : tns) {
             //   System.out.println(tn.prettyPrint(0));
            //}

            //System.out.println("--------------------------");
        }

        System.out.println("Total Tree Count: " + trees.size());
        System.out.println("Total Query Count: " + totalQueryCount);

        /*
        for (Set<Table> ts : qc.getEdges().edges().get(ec.getOutputTable())) {
            for (Table t : ts)
                System.out.println(t);
        }*/

        return qc;
    }

    public static void enumTableWithoutProj(EnumContext ec, QueryChest qc, int depth) {

        int lastQueryCount = 0;
        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumFilterNamed.emitEnumFilterNamed(ec, qc);

        System.out.println("[Stage 1] EnumFilterNamed: \n\t"
                + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        lastQueryCount = qc.queryCount;
        qc.tracked.clear();

        ec.setTableNodes(qc.getRepresentativeTableNodes());
        EnumAggrTableNode.emitEnumAggregationNode(ec, qc);

        System.out.println("[Stage 2] EnumAggregationNode: \n\t"
                + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
        lastQueryCount = qc.queryCount;
        qc.tracked.clear();

        for (int i = 1; i <= depth; i ++) {
            //System.out.println("[Level] " + i);

            ec.setTableNodes(qc.getRepresentativeTableNodes());

            EnumJoinTableNodes.emitEnumJoinWithFilter(ec, qc);
            //System.out.println("There are " + tns.size() + " queries in the enumeration of this level");
            //List<TableNode> renamed = tns.parallelStream().map(tn -> RenameTNWrapper.tryRename(tn)).collect(Collectors.toList());
            // System.out.println("After renamed: " + renamed.size());
            //qc.updateQueries(renamed);

            //System.out.println("after enumJoinWithFilter: " + qc.getRepresentativeTableNodes().size() + " tables");
            System.out.println("[Stage " + (2 + i) + "] EnumJoin" + i + " \n\t"
                    + "Queries generated: " + (qc.queryCount - lastQueryCount) + "\n\t"
                    + "Tables generated: " + (qc.tracked.size()) + "\n\t"
                    + "Total Table by now: " + qc.getRepresentativeTableNodes().size());
            lastQueryCount = qc.queryCount;
            qc.tracked.clear();
        }
    }
}
