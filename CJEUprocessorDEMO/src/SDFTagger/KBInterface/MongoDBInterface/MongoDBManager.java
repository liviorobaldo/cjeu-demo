package SDFTagger.KBInterface.MongoDBInterface;

import SDFTagger.SDFItems.*;
import SDFTagger.KBInterface.XMLFilesInterface.*;
import com.mongodb.*;
import java.util.*;
import java.io.File;
 
//This class compile the files and then load all information in MongoDB, to facilitate multi-thread, i.e., avoiding loading in RAM the same rules multiple times.

//It is not so fast ... I guess because every time the Java query the MongoDB and all these queries slow down performances. 
//Or maybe it's because I'm not practical with MongoDB ... I don't know how to set it up to speed up queries...

//The classe extends XMLFilesManager because it needs the method of XMLFilesManager to upload the SDFCode(s) from file.
//But, it override the methods fillBagsOfSDFNode and retrieveSDFCodes, that in this class must be based on data from MongoDB
public class MongoDBManager extends XMLFilesManager
{
    private DB mongoDBdatabase = null;
    
        //Constructor: (1) we build the Hashtable(s) and the ArrayList(s) of the XMLFilesManager (via the XMLFilesManager constructor), 
        //(2) we upload their content on the MongoDB, (3) we empty the Hashtable(s) and the ArrayList(s).
    public MongoDBManager
    (
            //for the bags ...
        File rootDirectoryBags, String[] localPathsBags,
            //for the XML files
        File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules,
            //If reloadKB is true, we create a MongoDB database with this name and we load the XML files there.
            //If it is false, we don't load; in such a case, it is assumed that the KB has been already loaded: we can directly use it
            //via the methods fillBagsOfSDFNode and retrieveSDFCodes defined below.
        String SDFTaggerKBname, boolean reloadKB
    )throws Exception
    {
        super(rootDirectoryBags, localPathsBags, rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
    
        Mongo mdb = new Mongo("localhost", 27017);
        mongoDBdatabase = mdb.getDB(SDFTaggerKBname);
        
        if(reloadKB==true)
        {
                //We create three collections in the DB, we reset them (i.e., we cancel the previous ones), and we index them to speed up search
            DBCollection allFormsToBags = mongoDBdatabase.getCollection("allFormsToBags");
            DBCollection allLemmasToBags = mongoDBdatabase.getCollection("allLemmasToBags");
            DBCollection rulesForm = mongoDBdatabase.getCollection("rulesForm");
            DBCollection rulesLemma = mongoDBdatabase.getCollection("rulesLemma");
            DBCollection rulesPOS = mongoDBdatabase.getCollection("rulesPOS");
            DBCollection rulesNotAssociatedWithFormsLemmasAndPOSs = mongoDBdatabase.getCollection("rulesNotAssociatedWithFormsLemmasAndPOSs");

            allFormsToBags.drop();
            allLemmasToBags.drop();
            rulesForm.drop();
            rulesLemma.drop();
            rulesPOS.drop();
            rulesNotAssociatedWithFormsLemmasAndPOSs.drop();

            allFormsToBags.createIndex(new BasicDBObject("Form", 1));
            allLemmasToBags.createIndex(new BasicDBObject("Lemma", 1));
            rulesForm.createIndex(new BasicDBObject("Form", 1));
            rulesLemma.createIndex(new BasicDBObject("Lemma", 1));
            rulesPOS.createIndex(new BasicDBObject("POS", 1));
            //rulesNotAssociatedWithFormsLemmasAndPOSs.createIndex(new BasicDBObject("id", 1));//we cannot index on the SDFCode ... so we add this (useless) field

            System.out.println("\tUploading Bag(s) indexed on Form(s)");
            Enumeration en = super.allFormsToBags.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                ArrayList<String> bags = super.allFormsToBags.get(key);
                for(int i=0;i<bags.size();i++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Form", key);
                    obj.put("bag", bags.get(i));
                    allFormsToBags.insert(obj);
                }
            }

            System.out.println("\tUploading Bag(s) indexed on Lemma(s)");
            en = super.allLemmasToBags.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                ArrayList<String> bags = super.allLemmasToBags.get(key);
                for(int i=0;i<bags.size();i++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Lemma", key);
                    obj.put("bag", bags.get(i));
                    allFormsToBags.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on Form(s)");
            for(int i=0;i<super.rulesFormIndex.size();i++)
            {
                String key = super.rulesFormIndex.get(i);
                ArrayList<String> rules = super.rulesForm.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Form", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesForm.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on Lemma(s)");
            for(int i=0;i<super.rulesLemmaIndex.size();i++)
            {
                String key = super.rulesLemmaIndex.get(i);
                ArrayList<String> rules = super.rulesLemma.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("Lemma", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesLemma.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) indexed on POS(s)");
            for(int i=0;i<super.rulesPOSIndex.size();i++)
            {
                String key = super.rulesPOSIndex.get(i);
                ArrayList<String> rules = super.rulesPOS.get(i);
                for(int j=0; j<rules.size(); j++)
                {
                    BasicDBObject obj = new BasicDBObject();
                    obj.put("POS", key);
                    obj.put("SDFCode", rules.get(j));
                    rulesPOS.insert(obj);
                }
            }

            System.out.println("\tUploading SDFRule(s) not indexed neither on Form(s) nor on Lemma(s) nor on POS(s)");

                //finally, in SDFRulesNotOnFormOrLemma we upload all SDFRule(s) not related to any Form and Lemma.
                //We'll have to check all these rules on all Head(s).
            for(int i=0;i<super.rulesNotAssociatedWithFormsLemmasAndPOSs.size();i++)
            {
                BasicDBObject obj = new BasicDBObject();
                //obj.put("SDFCode", "no-id");
                obj.put("SDFCode", super.rulesNotAssociatedWithFormsLemmasAndPOSs.get(i));
                rulesNotAssociatedWithFormsLemmasAndPOSs.insert(obj);
            }
        }
        
            //At the end, we delete the Hashtable(s) and ArrayList(s) of XMLFilesManager ... why should we keep them in RAM?
        super.allFormsToBags = null;
        super.allLemmasToBags = null;
        super.rulesFormIndex = null;
        super.rulesForm = null;
        super.rulesLemmaIndex = null;
        super.rulesLemma = null;
        super.rulesPOSIndex = null;
        super.rulesPOS = null;
        super.rulesNotAssociatedWithFormsLemmasAndPOSs = null;
        Runtime.getRuntime().gc();
    }
    
/************** METHODS OF THE interface KBInterface: overriden with respect to the ones in XMLFilesManager **************/
    public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception
    {
        DBCollection allFormsToBags = mongoDBdatabase.getCollection("allFormsToBags");
        DBCollection allLemmasToBags = mongoDBdatabase.getCollection("allLemmasToBags");
        
        BasicDBObject query = new BasicDBObject();
        query.put("Form", Form);
        DBCursor cur = allFormsToBags.find(query);
        if(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            bagsOnForm.add((String)dbobject.get("bag"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("Lemma", Lemma);
        cur = allLemmasToBags.find(query);
        if(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            bagsOnLemma.add((String)dbobject.get("bag"));
        }
        cur.close();
    }
   
    public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception
    {
        String Form = SDFHead.getForm().toLowerCase();
        String Lemma = SDFHead.getLemma().toLowerCase();
        String POS = SDFHead.getPOS().toLowerCase();
        
        ArrayList<String> ret = new ArrayList<String>();
        
        DBCollection rulesForm = mongoDBdatabase.getCollection("rulesForm");
        DBCollection rulesLemma = mongoDBdatabase.getCollection("rulesLemma");
        DBCollection rulesPOS = mongoDBdatabase.getCollection("rulesPOS");
        DBCollection rulesNotAssociatedWithFormsLemmasAndPOSs = mongoDBdatabase.getCollection("rulesNotAssociatedWithFormsLemmasAndPOSs");
        
        BasicDBObject query = new BasicDBObject();
        query.put("Form", Form);
        DBCursor cur = rulesForm.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("Lemma", Lemma);
        cur = rulesLemma.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        query = new BasicDBObject();
        query.put("POS", POS);
        cur = rulesPOS.find(query);
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        cur = rulesNotAssociatedWithFormsLemmasAndPOSs.find();
        while(cur.hasNext()==true)
        {
            DBObject dbobject = cur.next();
            ret.add((String)dbobject.get("SDFCode"));
        }
        cur.close();
        
        return ret;
    }
}