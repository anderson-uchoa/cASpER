package it.unisa.ascetic.storage.repository;

import it.unisa.ascetic.analysis.code_smell.BlobCodeSmell;
import it.unisa.ascetic.analysis.code_smell.CodeSmell;
import it.unisa.ascetic.analysis.code_smell.MisplacedClassCodeSmell;
import it.unisa.ascetic.storage.beans.*;
import it.unisa.ascetic.storage.beans.ClassBean;
import it.unisa.ascetic.storage.beans.InstanceVariableBean;
import it.unisa.ascetic.storage.beans.InstanceVariableListProxy;
import it.unisa.ascetic.storage.beans.MethodBean;
import it.unisa.ascetic.storage.beans.MethodListProxy;
import it.unisa.ascetic.storage.beans.PackageBean;
import it.unisa.ascetic.storage.sqlite_jdbc_driver_connection.SQLiteConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * repository addetta al salvataggio delle classi nel db
 */
public class ClassRepository implements ClassBeanRepository {

    private Connection con = null; // oggetto connessione per stabilire connessione al db
    private PreparedStatement stat = null; // variabile per creare statement da eseguire
    private ResultSet res = null; // resultset per memorizzare risultati della query

    /**
     * aggiunge una classe al database e riempie le tabelle contenenti le relazioni con metodi e
     * variabili di istanza  contenuti dalla classe da aggiungere
     *
     * @param aClass classe da aggiungere
     * @throws RepositoryException
     */
    @Override
    public void add(ClassBean aClass) throws RepositoryException {
        try {
            con = SQLiteConnector.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String sql = "INSERT OR REPLACE INTO ClassBean(fullQualifiedName,textContent,belongingPackage,LOC,superclass,entityClassUsage,pathToFile) VALUES(?,?,?,?,?,?,?)";
        List<InstanceVariableBean> instanceList = new ArrayList<InstanceVariableBean>();
        if (aClass.getInstanceVariablesList() != null) instanceList = aClass.getInstanceVariablesList();
        List<MethodBean> methodList = new ArrayList<MethodBean>();
        if (aClass.getMethodList() != null) methodList = aClass.getMethodList();

        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.setString(1, aClass.getFullQualifiedName());
            stat.setString(2, aClass.getTextContent());
            stat.setString(3, aClass.getBelongingPackage().getFullQualifiedName());
            stat.setInt(4, aClass.getLOC());
            if (aClass.getSuperclass() != null) {
                stat.setString(5, aClass.getSuperclass());
            } else {
                stat.setString(5, "");
            }
            ;
            stat.setInt(6, aClass.getEntityClassUsage());
            stat.setString(7, aClass.getPathToFile());
            stat.executeUpdate();

            for (InstanceVariableBean instance : instanceList) {
                addInstanceVariable(instance, con);
                sql = "INSERT OR REPLACE INTO Classe_VariabileIstanza(classBeanFullQualifiedName,instanceVariableBeanFullQualifiedName) VALUES (?,?)";
                stat = (PreparedStatement) con.prepareStatement(sql);
                stat.setString(1, aClass.getFullQualifiedName());
                stat.setString(2, instance.getFullQualifiedName());
                stat.executeUpdate();
            }
            ;
            for (MethodBean method : methodList) {
                addMethod(method, con);
            }
            con.commit();
            stat.close();
            SQLiteConnector.releaseConnection(con);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }
        ;
    }

    /**
     * metodo privato, utilizzato per aggiungere a cascata i metodi contenuti nella classe
     * da aggiungere al db e al contempo riempire le tabelle contenenti le variabili usate dal metodo e parametri passati al metodo nel db
     *
     * @param toAdd metodo da aggiungere al db
     * @param con   oggetto connessione
     * @throws RepositoryException
     */
    private void addMethod(MethodBean toAdd, Connection con) throws RepositoryException {
        String sql = "INSERT OR REPLACE INTO MethodBean(fullQualifiedName,textContent,return_type,staticMethod,isDefaultConstructor,belongingClass,visibility) VALUES (?,?,?,?,?,?,?)";
        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.setString(1, toAdd.getFullQualifiedName());
            stat.setString(2, toAdd.getTextContent());
            if (toAdd.getReturnType() != null) {
                stat.setString(3, toAdd.getReturnType().getFullQualifiedName());
            } else {
                stat.setString(3, "");
            }
            stat.setBoolean(4, toAdd.getStaticMethod());
            stat.setBoolean(5, toAdd.getDefaultCostructor());
            stat.setString(6, toAdd.getBelongingClass().getFullQualifiedName());
            stat.setString(7, toAdd.getVisibility());
            stat.executeUpdate();

            List<InstanceVariableBean> list = new ArrayList<InstanceVariableBean>();
            if (toAdd.getInstanceVariableList() != null) {
                list = toAdd.getInstanceVariableList();
                for (InstanceVariableBean instance : list) {
                    sql = "INSERT OR REPLACE INTO Instance_Variable_Used(methodBeanFullQualifiedName,instanceVariableBeanFullQualifiedName) VALUES (?,?)";
                    stat = (PreparedStatement) con.prepareStatement(sql);
                    stat.setString(1, toAdd.getFullQualifiedName());
                    stat.setString(2, instance.getFullQualifiedName());
                    stat.executeUpdate();
                }
            }

            HashMap<String, ClassBean> hash = toAdd.getParameters();
            if (hash != null) {
                Set<String> set = hash.keySet();
                for (String key : set) {
                    sql = "INSERT OR REPLACE INTO Parameter_Used(methodBeanFullQualifiedName,parameterClassFullQualifiedName,typeParameter,classBeanFullQualifiedName) VALUES (?,?,?,?)";
                    stat = (PreparedStatement) con.prepareStatement(sql);
                    stat.setString(1, toAdd.getFullQualifiedName());
                    stat.setString(2, key);
                    stat.setString(3, hash.get(key).getFullQualifiedName());
                    stat.setString(4, toAdd.getBelongingClass().getFullQualifiedName());
                    stat.executeUpdate();
                }
            }
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }

    }

    /**
     * metodo privato per aggiungere a cascata le variabili di istanza contenute dalla classe nel db
     *
     * @param toAdd variabile da aggiungere
     * @param con   oggetto connessione
     * @throws RepositoryException
     */
    private void addInstanceVariable(InstanceVariableBean toAdd, Connection con) throws RepositoryException {
        String sql = "INSERT OR REPLACE INTO InstanceVariableBean(fullQualifiedName,tipo,initialization,visibility) VALUES(?,?,?,?)";
        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.setString(1, toAdd.getFullQualifiedName());
            stat.setString(2, toAdd.getType());
            stat.setString(3, toAdd.getInitialization());
            stat.setString(4, toAdd.getVisibility());
            stat.executeUpdate();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }
        ;
    }

    /**
     * aggiorna una classe contenuta dal db con nuovi valori, aggiornando anche la tabella contenente informazioni sui code smell presenti in essa
     *
     * @param aClass classe aggiornata da reinserire
     * @throws RepositoryException
     */
    @Override
    public void update(ClassBean aClass) throws RepositoryException {
        try {
            con = SQLiteConnector.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String sql = "UPDATE ClassBean SET textContent=?, belongingPackage=?, LOC=?, superclass=?, entityClassUsage=?, pathToFile=? " +
                "WHERE fullQualifiedName=? ";
        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.setString(1, aClass.getTextContent());
            stat.setString(2, aClass.getBelongingPackage().getFullQualifiedName());
            stat.setInt(3, aClass.getLOC());
            if (aClass.getSuperclass() != null) {
                stat.setString(4, aClass.getSuperclass());
            } else {
                stat.setString(4, "");
            }
            ;
            stat.setInt(5, aClass.getEntityClassUsage());
            stat.setString(6, aClass.getPathToFile());
            stat.setString(7, aClass.getFullQualifiedName());
            stat.executeUpdate();

            String enviedClass = null;
            if (aClass.getEnviedPackage() != null) enviedClass = aClass.getEnviedPackage().getFullQualifiedName();

            List<CodeSmell> list = aClass.getAffectedSmell();
            if (list != null) {
                sql = "INSERT OR REPLACE INTO Index_CodeSmell (indexId,indice,name) VALUES (?,?,?);";
                String sql2 = "INSERT OR REPLACE INTO Classe_SmellType (classBeanFullQualifiedName,codeSmellFullQualifiedName,fqn_envied_package,algorithmUsed,indice) VALUES (?,?,?,?,?);";

                String key = null;
                HashMap<String, Double> index = null;
                for (CodeSmell smell : list) {
                    key = smell.getSmellName() + "-" +aClass.getFullQualifiedName();
                    stat = (PreparedStatement) con.prepareStatement(sql);

                    index = smell.getIndex();
                    for (String s : index.keySet()) {
                        stat.setString(1, key);
                        stat.setString(2, String.valueOf(index.get(s)));
                        stat.setString(3, s);

                        stat.executeUpdate();
                    }

                    stat = (PreparedStatement) con.prepareStatement(sql2);

                    stat.setString(1, aClass.getFullQualifiedName());
                    stat.setString(2, smell.getSmellName());
                    stat.setString(3, enviedClass);
                    stat.setString(4, smell.getAlgoritmsUsed());
                    stat.setString(5, key);
                    stat.executeUpdate();

                }
            }

            con.commit();
            stat.close();
            SQLiteConnector.releaseConnection(con);

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }
        ;
    }

    /**
     * rimuove una classe dal db
     *
     * @param aClass classe da rimuovere
     * @throws RepositoryException
     */
    @Override
    public void remove(ClassBean aClass) throws RepositoryException {
        try {
            con = SQLiteConnector.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String sql = "DELETE FROM ClassBean WHERE fullQualifiedName='" + aClass.getFullQualifiedName() + "'";

        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.executeUpdate();
            con.commit();
            stat.close();
            SQLiteConnector.releaseConnection(con);

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }
        ;
    }

    /**
     * pulisce tabella code smell della classe
     *
     * @throws RepositoryException
     */
    @Override
    public void delete() throws RepositoryException {
        try {
            con = SQLiteConnector.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String sql = "DELETE FROM Classe_SmellType";

        try {

            stat = (PreparedStatement) con.prepareStatement(sql);
            stat.executeUpdate();
            con.commit();
            stat.close();
            SQLiteConnector.releaseConnection(con);

        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }
        ;
    }

    /**
     * seleziona una lista di classi dal db
     *
     * @param criterion generica query per la selezione dal db
     * @return lista di classi selezionate dalla query
     * @throws RepositoryException
     */
    @Override
    public List<ClassBean> select(Criterion criterion) throws RepositoryException {
        try {
            con = SQLiteConnector.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        final List<ClassBean> classes = new ArrayList<ClassBean>();
        final SQLiteCriterion sqlCriterion = (SQLiteCriterion) criterion; // oggetto contenente la query da eseguire

        try {
            String key = null;
            String sql = sqlCriterion.toSQLquery();

            Statement selection = con.createStatement();
            res = selection.executeQuery(sql);
            ClassBean.Builder c = null;

            while (res.next()) {
                c = new ClassBean.Builder(res.getString("CfullQualifiedName"), res.getString("CtextContent"))
                        .setInstanceVariables(new InstanceVariableListProxy(res.getString("CfullQualifiedName")))
                        .setMethods(new MethodListProxy(res.getString("CfullQualifiedName")))
                        .setImports(new ArrayList<String>())
                        .setLOC(Integer.parseInt(res.getString("LOC")))
                        .setBelongingPackage(new PackageBean.Builder(res.getString("belongingPackage"), res.getString("PtextContent")).build())
                        .setEnviedPackage(null)
                        .setEntityClassUsage(Integer.parseInt(res.getString("entityClassUsage")))
                        .setPathToFile(res.getString("pathToFile"))
                        .setAffectedSmell();

                if (!res.getString("superclass").equals("")) {
                    c.setSuperclass(res.getString("superclass"));
                }

                classes.add(c.build());
            }

            for (ClassBean classe : classes) {
                sql = "SELECT codeSmellFullQualifiedName, fqn_envied_package, algorithmUsed, indice FROM Classe_SmellType WHERE classBeanFullQualifiedName='" + classe.getFullQualifiedName() + "'";
                res = selection.executeQuery(sql);
                while (res.next()) {
                    CodeSmell smell = null;
                    if (res.getString("codeSmellFullQualifiedName").equals(CodeSmell.MISPLACED_CLASS)) {
                        smell = new MisplacedClassCodeSmell(null, res.getString("algorithmUsed"));
                        if (res.getString("fqn_envied_package") != null) {
                            PackageBean enviedPackage = new PackageBean.Builder(res.getString("fqn_envied_package"), "").build();
                            classe.setEnviedPackage(enviedPackage);
                        }
                    } else if (res.getString("codeSmellFullQualifiedName").equalsIgnoreCase(CodeSmell.BLOB)) {
                        smell = new BlobCodeSmell(null, res.getString("algorithmUsed"));
                    }

                    classe.addSmell(smell);
                }
            }

            for (ClassBean classBean : classes) {
                if (classBean.getEnviedPackage() != null) {
                    String enviedPackageQuery = "SELECT * FROM PackageBean WHERE fullQualifiedName = '" + classBean.getEnviedPackage().getFullQualifiedName() + "'";
                    res = selection.executeQuery(enviedPackageQuery);
                    while (res.next()) {
                        classBean.setEnviedPackage(new PackageBean.Builder(res.getString("fullQualifiedName"), res.getString("textContent")).build());
                    }
                }

                for(CodeSmell smell : classBean.getAffectedSmell()){
                    key = smell.getSmellName() + "-" + classBean.getFullQualifiedName();
                    sql = "SELECT indice, name FROM Index_CodeSmell WHERE indexId='" + key + "'";
                    res = selection.executeQuery(sql);
                    while (res.next()) {
                        smell.addIndex(res.getString("name"), Double.parseDouble(res.getString("indice")));
                    }

                }
            }

            con.commit();
            res.close();
            selection.close();
            SQLiteConnector.releaseConnection(con);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new RepositoryException(ex.getMessage());
            }
            throw new RepositoryException(e.getMessage());
        }

        return classes;
    }

}
