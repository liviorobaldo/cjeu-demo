package SDFTagger.KBInterface.SDFCodesInterface;
import SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler.convert2SDFCode;
import SDFTagger.KBInterface.XMLFilesInterface.XMLFilesManager;
import java.io.StringReader;
import java.util.*;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

public class SDFCodesManager extends XMLFilesManager
{
    public SDFCodesManager(ArrayList<String> SDFCodes)throws Exception
    {
        super(null, new String[0], null, null, new String[0]);
        
        setSDFCodes(SDFCodes);
    }
    

    private void setSDFCodes(ArrayList<String> SDFCodes)throws Exception
    {
        rulesFormIndex.clear();
        rulesForm.clear();
        rulesLemmaIndex.clear();
        rulesLemma.clear();
        rulesPOSIndex.clear();
        rulesPOS.clear();
        rulesNotAssociatedWithFormsLemmasAndPOSs.clear();
        

        for(int i=0;i<SDFCodes.size();i++)
        {
            String newSDFCode = SDFCodes.remove(i);
            newSDFCode = (i+1)+newSDFCode.substring(newSDFCode.indexOf("£"), newSDFCode.length());
            SDFCodes.add(i, newSDFCode);
            indexSDFRuleOnFormLemmaPOS(newSDFCode);
        }
    }
    

    private void setSDFBags(Hashtable<String, ArrayList<String>> allFormsToBags, Hashtable<String, ArrayList<String>> allLemmasToBags)
    {
        this.allFormsToBags.clear();
        this.allLemmasToBags.clear();
        for(String Form:allFormsToBags.keySet())this.allFormsToBags.put(Form, allFormsToBags.get(Form));
        for(String Lemma:allLemmasToBags.keySet())this.allLemmasToBags.put(Lemma, allLemmasToBags.get(Lemma));
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static void main1(String args[]) throws Exception 
    {
        try
        {
            SAXBuilder sb= new SAXBuilder();

            String docString = 
                "<SDFRule id=\"1\" priority=\"40\">\n" +
                "  <tag>TOPtag</tag>\n" +
                "  <headAlternatives>\n" +
                "    <head>\n" +
                "      <Lemma>TOP</Lemma>\n" +
                "    </head>\n" +
                "  </headAlternatives>\n" +
                "  <prevAlternatives>\n" +
                "    <prev maxDistance=\"1\">\n" +
                "      <headAlternatives>\n" +
                "        <head>\n" +
                "          <Form>Prev</Form>\n" +
                "        </head>\n" +
                "      </headAlternatives>\n" +
                "       <prevAlternatives>\n" +
                "           <prev maxDistance=\"1\">\n" +
                "               <headAlternatives>\n" +
                "                   <head>\n" +
                "                       <Form>PrevPrev</Form>\n" +
                "                   </head>\n" +
                "               </headAlternatives>\n" +
                "           </prev>\n" +
                "       </prevAlternatives>\n" +
                "    </prev>\n" +
                "  </prevAlternatives>\n" +
                "</SDFRule>";
            
            Document doc = sb.build(new StringReader(docString));
            convert2SDFCode convert2SDFCode = new convert2SDFCode(doc.getRootElement());
            System.out.println(convert2SDFCode.getSDFCode());
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    public static void main(String args[]) throws Exception 
    {
        try
        {
            ArrayList<String> codes = new ArrayList<String>();
            
            Hashtable<String, String> pairsSlotValue = new Hashtable<String, String>();
            pairsSlotValue.put("Form", "TOP");
            SDFCodeFactory SDFCodeFactory = new SDFCodeFactory(40, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule", "TOPtag");
                        
            pairsSlotValue.clear();
            pairsSlotValue.put("POS", "NOUN");
            pairsSlotValue.put("Form", "Prev");
            SDFCodeFactory.addPrev("SDFRule", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            /**/
            pairsSlotValue.clear();
            pairsSlotValue.put("Lemma", "provaPrev2");
            pairsSlotValue.put("Form", "PrevPrev");
            SDFCodeFactory.addPrev("SDFRule/Prev[1]", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            /**/
            SDFCodeFactory.addSDFTag("SDFRule/Prev[1]/Prev[1]", "Eccoci");
            /**/
            
            codes.add(SDFCodeFactory.getSDFCode());
            
            pairsSlotValue.clear();
            pairsSlotValue.put("Lemma", "secondo");
            pairsSlotValue.put("Form", "me");
            SDFCodeFactory = new SDFCodeFactory(13, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule", "SempreTOP");
                        
            pairsSlotValue.clear();
            pairsSlotValue.put("Form", "Next");
            pairsSlotValue.put("POS", "UFFA");
            SDFCodeFactory.addNext("SDFRule", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            pairsSlotValue.clear();
            pairsSlotValue.put("Form", "NextStart");
            SDFCodeFactory.addNextStar("SDFRule/Next[1]", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule/Next[1]/NextStar[1]", "Finoaqui");
            
            codes.add(SDFCodeFactory.getSDFCode());
            
            
            SDFCodesManager SDFCodesManager = new SDFCodesManager(codes);
            
            SDFCodesManager = SDFCodesManager;
            
        }
        catch(Exception e)
        {
            System.out.println("Eccezione! Message: \""+e.getMessage()+"\"");
        }
    }
}