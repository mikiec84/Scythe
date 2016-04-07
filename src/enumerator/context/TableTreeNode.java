package enumerator.context;

import enumerator.primitive.OneStepQueryInference;
import sql.lang.Table;
import sql.lang.ast.table.NamedTable;
import sql.lang.ast.table.TableNode;
import util.Pair;
import util.RenameTNWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The data structure used to store how a table can be generated from other tables.
 *          node
 *          /   \
 *       node1  node2
 * indicates that the 'node' can be generated from node1 and node 2
 * Each table tree corresponds to a bunch of SQL queries to generate the root node table.
 * Created by clwang on 4/7/16.
 */
public class TableTreeNode {
    Table node;
    List<TableTreeNode> children = new ArrayList<>();

    // an uniformed encoding for childrens
    List<TableNode> childrenEncoding = new ArrayList<>();

    // this field is invalid empty until "treeToQuery" is called
    List<TableNode> queries = new ArrayList<>();

    public TableTreeNode(Table node, List<TableTreeNode> children) {
        this.node = node;
        this.children = children;
        this.childrenEncoding = children.stream().map(t -> new NamedTable(t.getTable())).collect(Collectors.toList());
    }

    public Table getTable() {
        return this.node;
    }

    public List<TableTreeNode> getChildren() { return this.children; }

    // test if all of the leaf nodes of the tree are valid.
    public boolean leafValid(Set<Table> validLeaves) {
        if (this.children.size() == 0) {
            return validLeaves.contains(this.node);
        } else {
            boolean valid = true;
            for (TableTreeNode ttn : children) {
                valid = valid && ttn.leafValid(validLeaves);
            }
            return valid;
        }
    }

    public void print(int depth) {
        String indent = "";
        for (int i = 0; i < depth; i ++) {
            indent += "    ";
        }
        System.out.println(this.node.toStringWithIndent(indent));
        for (TableTreeNode ttn : this.children) {
            ttn.print(depth + 1);
        }
    }

    public void inferQuery(EnumContext ec) {
        ec.setTableNodes(childrenEncoding);
        List<TableNode> tns = OneStepQueryInference.infer(childrenEncoding, this.getTable(), ec);
        this.queries = tns;

        for (TableTreeNode ttn : this.children) {
            ttn.inferQuery(ec);
        }
    }

    // translate an tree to a set of sql queries
    public List<TableNode> treeToQuery() {
        List<TableNode> result = new ArrayList<>();

        // this is a leaf node
        if (this.children.size() == 0) {
            result.add(new NamedTable(this.node));
            return result;
        }

        List<List<TableNode>> horizontalSelections = new ArrayList<>();
        horizontalSelections.add(new ArrayList<>());

        for (TableTreeNode ttn : this.children) {
            List<List<TableNode>> newHS = new ArrayList<>();
            List<TableNode> tns = ttn.treeToQuery();
            for (List<TableNode> hs : horizontalSelections) {
                for (TableNode tn : tns) {
                    List<TableNode> updated = new ArrayList<>();
                    updated.addAll(hs);
                    updated.add(tn);
                    newHS.add(updated);
                }
            }
            horizontalSelections = newHS;
        }

        for (TableNode q : this.queries) {
            for (List<TableNode> selection : horizontalSelections) {
                List<Pair<TableNode, TableNode>> substPair = new ArrayList<>();
                for (int i = 0; i < this.childrenEncoding.size(); i ++) {
                    substPair.add(new Pair<>(childrenEncoding.get(i), RenameTNWrapper.tryRename(selection.get(i))));
                }
                result.add(q.tableSubst(substPair));
            }
        }
        return result;
    }

}
