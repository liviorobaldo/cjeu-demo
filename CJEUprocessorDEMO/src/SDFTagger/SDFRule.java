package SDFTagger;

import java.util.*;
import org.jdom2.*;
import SDFTagger.SDFItems.*;

public class SDFRule 
{
        //The class SDFNodeConstraints clusters the non-recursive attributes of the SDFRule (or its nested class). SDFNodeConstraints also contains methods to enforce 
        //the checkings with respect to specific SDFNode(s) (read more comments within the class itself). Nevertheless, the object that will be instantiated (within 
        //the constructor of SDFRuel) on the the attribute SDFNodeConstraintsFactory is used only to *GENERATE* new SDFNodeConstraints(s), one for each <headAlternatives>
        //in SDFRule or in the nested classes Prev, Next, Dependent, and Governor.
        //In other words, on the object SDFNodeConstraintsFactory only the method FactorySDFHeadConstraints is called; this method generates and returns a new object
        //of type SDFNodeConstraints.
        
        //The reason of this workflow is that SDFNodeConstraints may be extended in subclasses implementing constraints on the optionalFeatures. In such a case, the 
        //constructor of SDFRule will get in input an object of the extended class (that will be stored in SDFNodeConstraintsFactory), one instance of this object 
        //will be generated and associated with each headAlternative and extended checks on the optional features will be enforced.
    protected SDFNodeConstraints SDFNodeConstraintsFactory = null;
    
        //Each SDFRule has an id, and idInstance, and a priority (note these are private, i.e., only belonging to SDFRule) and the attribute below (protected: they must be read also in 
        //the nested subclasses Prev, Next, Dependent, Governor).
        //idInstance is given in the constructor: it is needed to distinguish between different instances of SDFRule(s) with the same id. Both the id and the 
        //idInstance is written in the Tag(s).
    protected long id = 0; 
    protected long priority = 0;
    
        //This is the SDFNodeConstraints that will be created via SDFNodeConstraintsFactory.FactorySDFNodeConstraints within the method parseSDFCode below.
    protected SDFNodeConstraints SDFNodeConstraints = null;
             
        //The ArrayList below are for the recursions left, right, up, and down. "owner" is the SDFRule where the Prev, Next, Governor, and Dependent belong,
        //possibly recursively. The owner of an (root) SDFRule is itself. The owner of a Prev, Next, Governor, and Dependent in the ArrayList or in the ArrayList
        //recursively found from them is the root SDFRule. 
    protected SDFRule owner = null;
    protected ArrayList<Prev> prevStarAlternatives = new ArrayList<Prev>();
    protected ArrayList<Prev> prevAlternatives = new ArrayList<Prev>();
    protected ArrayList<Next> nextStarAlternatives = new ArrayList<Next>();
    protected ArrayList<Next> nextAlternatives = new ArrayList<Next>();
    protected ArrayList<ArrayList<Dependent>> dependentsAlternatives = new ArrayList<ArrayList<Dependent>>();
    protected ArrayList<Governor> governorAlternatives = new ArrayList<Governor>();

        //There is a maximum limit on the Star. For now it is static in these attributes; maybe in the future
        //I'll put an "options" attribute on <prevStarAlternatives> and <nextStarAlternatives>
    protected final int maxStarRangeInitValue=10000;
    protected int maxStarRange=10000;
    
        //These are attributes which are filled during the execution of the SDFRule. The stack contains the tags collected through the several branches. If a branch
        //fails, the tags collected through that branch are rollbacked (i.e., removed); new tags will be possibly collected through other branches. The tags of the 
        //first branch that is satisfied are committed, i.e., migrated in the lower level of the stack.
        //At the end of the execution of the rules the tags collected through the execution of the SDFRule are on the top of the stack.
    protected ArrayList<ArrayList<SDFTag>> stack = new ArrayList<ArrayList<SDFTag>>();
    protected void commit(){ArrayList<SDFTag> tags=stack.remove(0);for(int i=0;i<tags.size();i++)stack.get(0).add(tags.get(i));}
    protected void rollback(){stack.remove(0);}
    
        //These are needed to properly execute PrevStar and NextStar, which can executed multiple time and each time the execuion must continue from the either the 
        //leftmost (PrevStar) or rightmost (NextStar) SDFNode. Each Prev|Next allow to memorize the leftmost|rightmost SDFNode touched in a special attribute. 
        //In these ArrayList we memorize the Prev and Next that need so. At the end of checkConstraints, before returning true, we check whether the SDFNode where  
        //the checkConstraints has been executed has index lower|higher than the one stored in leftmostSDFNode|rightmostSDFNode, for each Prev|Next in the ArrayList(s).
        //If so, we replace it. 
    protected ArrayList<Prev> prevsInPrevStarsCurrentlyInExecution = new ArrayList<Prev>();
    protected ArrayList<Next> nextsInNextStarsCurrentlyInExecution = new ArrayList<Next>();

        //saveSDFCode is used only for the nested classes Prev, Next, Dependent, and Governor. Read two rows below, before the line "if(SDFCode==null)return;"
    protected String saveSDFCode = null;
    protected SDFTagger SDFTagger = null;
    protected SDFLogger SDFLogger = null;
    public SDFRule(String SDFCode, SDFNodeConstraints SDFNodeConstraintsFactory, SDFTagger SDFTagger) throws Exception
    {   
            //When this constructor is called from the nested classes Prev, Next, Governor, and Dependent, we pass SDFCode=null, and the code below is not executed. 
            //Indeed, within the nested classes the SDFCode is parsed ONLY WHEN the method "protected boolean executeRule(SDFNode node) throws Exception" is executed 
            //(for the sake of efficiency: the nested classes are not always executed, if the rules fails on one of its branches, the other branches are not executed, 
            //so that it is useless to parse the SDFCode). Therefore, the nested classes do not parse the SDFCode in the constructor; rather, they store it in the 
            //attribute saveSDFCode and they will parse it only the first time executeRule will be called.
        if(SDFCode==null)return;
        
        this.owner = this;
        this.SDFNodeConstraintsFactory = SDFNodeConstraintsFactory;
        this.SDFTagger = SDFTagger;
        this.SDFLogger = SDFTagger.SDFTaggerConfig.SDFLogger;
        
            //We extract id and priority (these are on SDFRule only, not on the nested classes Prev, Next, Dependent, and Governor)
        id = Long.parseLong(SDFCode.substring(0, SDFCode.indexOf("£")));
        SDFCode = SDFCode.substring(SDFCode.indexOf("£")+1, SDFCode.length());
        priority = Long.parseLong(SDFCode.substring(0, SDFCode.indexOf("£")));
        SDFCode = SDFCode.substring(SDFCode.indexOf("£")+1, SDFCode.length());
        
            //We parse the SDFCode, which will fill headAlternatives, endOfSentence, headId, and tags. On the other hand,
            //we extract the SDFCode for Prev, Next, Dependent, and Governor. But we don't instantiate their argument (and
            //recursive Prev, Next, Dependent, and Governor). This will be instantiated only when executing the nested class.
            //That's why we need the attribute saveCode. Therefore, if an SDFRule fails on the main <headAlternative>, the 
            //SDFCode of the Prev, Next, Dependent, and Governor will not be instantiated.
        parseSDFCode(SDFCode);
    }

        //This is the main method of the SDFRule. It execute it on a specific SDFNode and returns the list of Tag(s) found during the execution of the SDFRule
        //or "null" if the SDFRule fails. As you can see, this method call the protected method checkConstraints, which verify all constraints of the rules are
        //satisfied. While these checks are performed, outputTags is filled, and it will be returned at the end. If at least one constraint fails, null is returned
    public ArrayList<SDFTag> executeSDFRule(SDFNode node, long idInstance) throws Exception
    {
        stack.clear();
        if(checkConstraints(node, idInstance)==true)return stack.get(0);
        return new ArrayList<SDFTag>();
    }

    protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
    {
        //System.out.println(SDFNode.SDFHead.getForm());
        
            //We put the tags on the top of the stack. If the SDFRule will be satisfied, these tags will be committed (i.e., copies on the lower level
            //of the stack), otherwise we'll rollback (i.e., remove them from the stack).
            //Both commit and rollback are done in the checkConstraints method of the nested classes Prev, Next, Dependent, and Governor. Not on the main
            //class SDFRule: its set of tags is the one that remains on the stack, i.e., the one that will be returned.
        ArrayList<SDFTag> tagsOnSDFNode = new ArrayList<SDFTag>();
        for(int i=0;i<SDFNodeConstraints.tags.size();i++)tagsOnSDFNode.add(new SDFTag(SDFNodeConstraints.tags.get(i), SDFNode.SDFHead, priority, id, idInstance));
        stack.add(0, tagsOnSDFNode);//we add ON THE TOP of the stack, i.e., at *ZERO*
        
            //We check constraints on this node of the SDFRule.
        if(SDFNodeConstraints.doesSDFNodeMatch(SDFNode)==false)
        {
                //For the SDFLogger: we start and we end a Step on a par. But only if we're not on the main SDFRule.
                //If we are on a main SDFRule, the SDFRule should have not been assigned to this SDFNode in the first place, but it was impossible to check 
                //that in XMLFilesManager, because we didnd't have the proper SDFNodeConstraints at disposal yet.
            String className = this.getClass().getName();
            if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
            if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
            if(className.compareToIgnoreCase("SDFRule")==0)return false;
            
            SDFLogger.startTracingOnSDFNode(this, idInstance, SDFNode);
            SDFLogger.endTracing(this, false);
            return false;
        }
        
            //For the SDFLogger: We start tracing the SDFRule (or nested subclass Prev, Next, Governor, Dependent
        SDFLogger.startTracingOnSDFNode(this, idInstance, SDFNode);
        
            //PrevStar
            //PrevStar are optional: even if all PrevStar fail, we continue with the execution of the SDFRule.
            //And, the Prev(s) in PrevStar can be executed multiple times, and the execution continues from the leftmost SDFNode: we memorize
            //the Prev in prevsInPrevStarsCurrentlyInExecution, so that, during the recursion, the leftmost SDFNode is memorized in the Prev.
            //The tags of the first Prev that is satisfied are committed on the stack.
        SDFNode prevSDFNode = SDFNode;
        if(prevStarAlternatives.size()>0)SDFLogger.stepInto(this, "prevStarAlternatives");//for the SDFLogger
        for(int i=0;i<prevStarAlternatives.size();i++)
        {   
            Prev prev = prevStarAlternatives.get(i);
            prevsInPrevStarsCurrentlyInExecution.add(prev);
            boolean ok = prev.checkConstraints(prevSDFNode, idInstance);
            prevsInPrevStarsCurrentlyInExecution.remove(prev);
            
                //If the prevStar succeeded...
            if(ok==true)
            {
                prevSDFNode=prev.leftmostSDFNode;//we continue from the leftmost SDFNode.
                i=-1;//we restart the execution from the first PrevStar(s)
            }
            
                //If this holds, we've reached the maximum number of iterations: we stop the PrevStar
            maxStarRange--;
            if(maxStarRange==0)break;
        }
        maxStarRange=maxStarRangeInitValue;
        if(prevStarAlternatives.size()>0)SDFLogger.stepOut();//for the SDFLogger
        
        
            //Prev
            //Contrary to PrevStar, at least one Prev must be satisfied. And, we return the tags of the first Prev that is satisfied.
        boolean ok = false;
        if(prevAlternatives.size()>0)SDFLogger.stepInto(this, "prevAlternatives");//for the SDFLogger
        for(int i=0;(i<prevAlternatives.size())&&(ok==false);i++)ok=prevAlternatives.get(i).checkConstraints(prevSDFNode, idInstance);
        if(prevAlternatives.size()>0)SDFLogger.stepOut();//for the SDFLogger
        if((ok == false)&&(prevAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }
        

            //NextStar
            //NextStar are optional: even if all NextStar fail, we continue with the execution of the SDFRule.
            //And, the Next(s) in NextStar can be executed multiple times, and the execution continues from the rightmost SDFNode: we memorize
            //the Next in nextsInNextStarsCurrentlyInExecution, so that, during the recursion, the rightmost SDFNode is memorized in the Next.
            //The tags of the first Next that is satisfied are committed on the stack.
        SDFNode nextSDFNode = SDFNode;
        if(nextStarAlternatives.size()>0)SDFLogger.stepInto(this, "nextStarAlternatives");//for the SDFLogger
        for(int i=0;i<nextStarAlternatives.size();i++)
        {
            Next next = nextStarAlternatives.get(i);
            nextsInNextStarsCurrentlyInExecution.add(next);
            ok = next.checkConstraints(nextSDFNode, idInstance);
            nextsInNextStarsCurrentlyInExecution.remove(next);
            
                //If the nextStar succeeded...
            if(ok==true)
            {
                nextSDFNode=next.rightmostSDFNode;//we continue from the rightmost SDFNode.
                i=-1;//we restart the execution from the first PrevStar(s)
            }
            
                //If this holds, we've reached the maximum number of iterations: we stop the PrevStar
            maxStarRange--;
            if(maxStarRange==0)break;
        }
        maxStarRange=maxStarRangeInitValue;
        if(nextStarAlternatives.size()>0)SDFLogger.stepOut();//for the SDFLogger
        
        
            //Next
            //Contrary to NextStar, at least one Next must be satisfied. And, we return the tags of the first Next that is satisfied.
        ok = false;
        if(nextAlternatives.size()>0)
            SDFLogger.stepInto(this, "nextAlternatives");//for the SDFLogger
        for(int i=0;(i<nextAlternatives.size())&&(ok==false);i++)
            ok=nextAlternatives.get(i).checkConstraints(nextSDFNode, idInstance);
        if(nextAlternatives.size()>0)
            SDFLogger.stepOut();//for the SDFLogger
        if((ok==false)&&(nextAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }

            //Before executing governorAlternatives and dependentsAlternatives, we save the values of prevsInPrevStarsCurrentlyInExecution and 
            //nextsInNextStarsCurrentlyInExecution and we clear these ArrayList(s): when we execute governorAlternatives|dependentsAlternatives
            //we are moving up|down, so that the leftmost|rightmost in PrevStar|NextStar need to be reset. Once we will have executed both
            //governorAlternatives and dependentsAlternatives is satisfied, we will restore these ArrayList(s).
        ArrayList<Prev> tempSavePrev = new ArrayList<Prev>(); 
        for(int i=0;i<prevsInPrevStarsCurrentlyInExecution.size();i++)tempSavePrev.add(prevsInPrevStarsCurrentlyInExecution.get(i));
        prevsInPrevStarsCurrentlyInExecution.clear();
        ArrayList<Next> tempSaveNext = new ArrayList<Next>(); 
        for(int i=0;i<nextsInNextStarsCurrentlyInExecution.size();i++)tempSaveNext.add(nextsInNextStarsCurrentlyInExecution.get(i));
        nextsInNextStarsCurrentlyInExecution.clear();

        
            //Governor
        ok = false;
        if(governorAlternatives.size()>0)SDFLogger.stepInto(this, "governorAlternatives");//for the SDFLogger
        for(int i=0;(i<governorAlternatives.size())&&(ok==false);i++)ok=governorAlternatives.get(i).checkConstraints(SDFNode, idInstance);
        if(governorAlternatives.size()>0)SDFLogger.stepOut();//for the SDFLogger
        if((ok==false)&&(governorAlternatives.size()>0))
        {
                //For the SDFLogger (the second parameter is null to signal the SDFLogger that the condition failed)
            SDFLogger.endTracing(this, false);
            return false;
        }
        
        
        
            //Dependents
        ok = false;
        if(dependentsAlternatives.size()>0)SDFLogger.stepInto(this, "dependentsAlternatives");//for the SDFLogger
        for(int i=0; (i<dependentsAlternatives.size())&&(ok==false); i++)
        {
                //We copy the tuple of Dependent(s) and we run the procedure matchTuplesOfDependents. This procedure looks for a tuple
                //of SDFHead[] included in SDFNode.SDFHead.getDependents() that satisfies the ArrayList<Dependent>.
                //If more than one tuple satisfies the ArrayList<Dependent>, it selects the first that it finds. Once a tuple is selected
                //all tags assigned in the recursions are committed on the stack.
            ArrayList<Dependent> dependents = new ArrayList<Dependent>();
            for(int j=0; j<dependentsAlternatives.get(i).size(); j++)dependents.add(dependentsAlternatives.get(i).get(j));
            
                //With matchTuplesOfDependents, we have an additional step into.
            SDFLogger.stepInto(this, "dependents");
            ok = matchTuplesOfDependents(dependents, SDFNode, id, idInstance, priority);
            SDFLogger.stepOut();
        }
        if(dependentsAlternatives.size()>0)SDFLogger.stepOut();//for the SDFLogger
        if((ok==false)&&(dependentsAlternatives.size()>0))
        {
            SDFLogger.endTracing(this, false);
            return false;
        }

            //We restore prevsInPrevStarsCurrentlyInExecution and nextsInNextStarsCurrentlyInExecution (that now are empty ... I mean: they *should* come 
            //back empty from dependentsAlternatives and governorAlternatives, if not there is some conceptual error somewhere...)
        for(int i=0;i<tempSavePrev.size();i++)prevsInPrevStarsCurrentlyInExecution.add(tempSavePrev.get(i));
        for(int i=0;i<tempSaveNext.size();i++)nextsInNextStarsCurrentlyInExecution.add(tempSaveNext.get(i));
        
            //If we arrived until here, the SDFRule satisfies the SDFNode in input. Or, if we are in a branch, this branch satisfy the SDFNode in input.
            //However, before returning true, we check the ArrayList(s) prevsInPrevStarsCurrentlyInExecution and nextsInNextStarsCurrentlyInExecution.
            //In case we are in a recursion of a PrevStar or of a NextStar, these ArrayList contain the Prev or Next for which we must memorize the  
            //leftmost or rightmost SDFNode. If the SDFRule is well-formed (we don't have a Next within a Prev, for instance), the present node is  
            //already the leftmost or the rightmost SDFNode.
        for(int i=0;i<prevsInPrevStarsCurrentlyInExecution.size();i++)
            if((prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode==null)||(SDFNode.index<prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode.index))
                prevsInPrevStarsCurrentlyInExecution.get(i).leftmostSDFNode=SDFNode;
        for(int i=0;i<nextsInNextStarsCurrentlyInExecution.size();i++)
            if((nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode==null)||(SDFNode.index>nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode.index))
                nextsInNextStarsCurrentlyInExecution.get(i).rightmostSDFNode=SDFNode;
        
        SDFLogger.endTracing(this, true);
        return true;
    }
    
/********************************************************************************************************************************************************/
/***************************************************************** NESTED CLASSES ***********************************************************************/
/********************************************************************************************************************************************************/
    protected class Prev extends SDFRule
    {
        protected int maxDistance = -1;
        protected boolean not = false;
        protected SDFNode leftmostSDFNode = null;
        
        public Prev(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);//we bypass SDFRule's constructor (read above at "public SDFRule(...) throws Exception" why)
                   
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            
                //We store the SDFCode in this attribute; we'll parse it only if/when checkConstraints will called for the first time
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
                //In the constructor we saved the SDFCode, because we parse it only if it is needed.
                //Now, it is needed.
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            
                //is there a previous-SDFNode? Is this perhaps required?
            if((SDFNode.prevSDFNode==null)||(SDFNode.prevSDFNode.SDFHead==null))
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                
                    //if not=true, we are asking the non-existence of a Prev. This is indeed met in case there are not previous SDFNode(s).
                    //otherwise it is false: we ask the existence of a certain Prev but there are not previous SDFNode(s).
                    //In both cases, the returning value is the value of "not".
                SDFLogger.endTracing(this, not);
                return not;
            }
            
                //This procedure returns all preceding SDFNode(s) at maxDistance.
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDistance(SDFNode, maxDistance);
            
            SDFNode prevSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(prevSatisfyingSDFNode==null); i++)
            {
                    //If the super.method is true, we save the SDFNode and we exit; in case not==false, we'll commit and return true
                    //If the super.method is false, we rollback, i.e., we remove these tags and we try on the next SDFNode, if any.
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)prevSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            
                //if no previous SDFNode satisfies the condition, but the "not" is true, it is actually require that any SDFNode satisfies the condition! We return true.
                //Instead, if not==false, we return false: we had to satisfy the condition, but no previous SDFNode(s) does.
                //So, in both cases, the returning value is the value of "not".
            if(prevSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                    //If a previous SDFNode satisfies the condition, and "not" is true ... we then return false. But before we have to rollback, as the tags
                    //on prevSatisfyingSDFNode are on the top of the stack! Otherwise (if not==false), we return true, but for the same reason, we first have 
                    //to commit, i.e., moving the tags of prevSatisfyingSDFNode to the lower level in the stack.
                    //So, in both cases, the returning value is the *NEGATION* of "not"
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }

            //This procedure returns all preceding SDFNode(s) at maxDistance.
        private ArrayList<SDFNode> getSDFNodesAtMaxDistance(SDFNode SDFNode, int distance)
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
            
            SDFNode tempSDFNode = SDFNode.prevSDFNode;
                //se tempNode.head==null we're on FirstSDFNode!
            while((tempSDFNode!=null)&&(distance>0)&&(tempSDFNode.SDFHead!=null))
            {
                ret.add(tempSDFNode);
                tempSDFNode = tempSDFNode.prevSDFNode;
                distance--;
            }
                
            return ret;
        }
        
            //This procudere has been overidden because here we also need to parse not and maxDistance
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)//not
            {
                not=true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)//maxDistance
            {
                maxDistance = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
                super.parseSDFCode(SDFCode);
            }
            if(maxDistance==-1)
                throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDistance");
        }
    }

    protected class Next extends SDFRule
    {
        protected int maxDistance = -1;
        protected boolean not = false;        
        protected SDFNode rightmostSDFNode = null;
        
        public Next(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);//we bypass SDFRule's constructor (read above at "public SDFRule(...) throws Exception" why)
                   
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            
                //We store the SDFCode in this attribute; we'll parse it only if/when checkConstraints will called for the first time
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
                //In the constructor we saved the SDFCode, because we parse it only if it is needed.
                //Now, it is needed.
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            
                //is there a next-SDFNode? Is this perhaps required?
            if((SDFNode.nextSDFNode==null)||(SDFNode.nextSDFNode.SDFHead==null))
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                
                    //if not=true, we are asking the non-existence of a Next. This is indeed met in case there are not previous SDFNode(s).
                    //otherwise it is false: we ask the existence of a certain Next but there are not previous SDFNode(s).
                    //In both cases, the returning value is the value of "not".
                SDFLogger.endTracing(this, not);
                return not;
            }
            
                //This procedure returns all preceding SDFNode(s) at maxDistance.
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDistance(SDFNode, maxDistance);
            
            SDFNode nextSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(nextSatisfyingSDFNode==null); i++)
            {
                    //If the super.method is true, we save the SDFNode and we exit; in case not==false, we'll commit and return true
                    //If the super.method is false, we rollback, i.e., we remove these tags and we try on the next SDFNode, if any.
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)nextSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            
                //if no subsequent SDFNode satisfies the condition, but the "not" is true, it is actually require that any SDFNode satisfies the condition! We return true.
                //Instead, if not==false, we return false: we had to satisfy the condition, but no previous SDFNode(s) does.
                //So, in both cases, the returning value is the value of "not".
            if(nextSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                    //If a previous SDFNode satisfies the condition, and "not" is true ... we then return false. But before we have to rollback, as the tags
                    //on prevSatisfyingSDFNode are on the top of the stack! Otherwise (if not==false), we return true, but for the same reason, we first have 
                    //to commit, i.e., moving the tags of prevSatisfyingSDFNode to the lower level in the stack.
                    //So, in both cases, the returning value is the *NEGATION* of "not"
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }
        
            //This procedure returns all subsequent SDFNode(s) at maxDistance.
        private ArrayList<SDFNode> getSDFNodesAtMaxDistance(SDFNode SDFNode, int distance)
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
            
            SDFNode tempSDFNode = SDFNode.nextSDFNode;
            while((tempSDFNode!=null)&&(distance>0))
            {
                ret.add(tempSDFNode);
                tempSDFNode = tempSDFNode.nextSDFNode;
                distance--;
            }
                
            return ret;
        }
        
            //This procudere has been overidden because here we also need to parse not and maxDistance
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)//not
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)//maxDistance
            {
                maxDistance = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
                super.parseSDFCode(SDFCode);
            }
            
            if(maxDistance==-1)
                throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDistance");
        }
    }
    
    protected class Governor extends SDFRule
    {
        protected int maxHeight = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();
        
        public Governor(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);//we bypass SDFRule's constructor (read above at "public SDFRule(...) throws Exception" why)
                   
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            
                //We store the SDFCode in this attribute; we'll parse it only if/when checkConstraints will called for the first time
            saveSDFCode = SDFCode;
        }
        
        protected boolean checkConstraints(SDFNode SDFNode, long idInstance)throws Exception
        {
                //In the constructor we saved the SDFCode, because we parse it only if it is needed.
                //Now, it is needed.
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            
                //is there a up-SDFNode? Is this perhaps required?
            if(SDFNode.SDFHead.getGovernor()==null)
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                
                    //if not=true, we are asking the non-existence of a Governor. This is indeed met in case this is the root.
                    //otherwise it is false: we ask the existence of a certain governor but there is not any governor.
                    //In both cases, the returning value is the value of "not".
                SDFLogger.endTracing(this, not);
                return not;
            }
            
            
                //This procedure returns all subsequent SDFNode(s) at maxHeight.    
                //This procedure returns all SDFNode(s), dependents of SDFNodeGovernor, at (maximum) maxHeight depth.
                //The procedure also fills the Hashtable SDFNodes2FirstDependent, which contain the association between the SDFNode(s) in SDFNodes
                //and the SDFNode immediately under him in the DependencyTree, in the subtree where the SDFNode as parameter is (we use this Hashtable
                //because a governor can have multiple dependents, but we need only the one where the SDFNode as parameter is to check the label.
            Hashtable<SDFNode,SDFNode> SDFNodes2FirstDependent = new Hashtable<SDFNode,SDFNode>();
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxHeight(SDFNode, maxHeight, SDFNodes2FirstDependent);
            
            SDFNode upSatisfyingSDFNode=null;
            for(int i=0; (i<SDFNodes.size())&&(upSatisfyingSDFNode==null); i++)
            {
                    //First we check the laber; if it fails, we can avoid rollbacking, because super.checkConstraints has not been done yet. We try the next in SDFNodes
                if(labelAlternatives.isEmpty()==false)
                {
                    boolean ok = false;
                    for(int j=0; (j<labelAlternatives.size())&&(ok==false); j++)
                        if(SDFNodes2FirstDependent.get(SDFNodes.get(i)).SDFHead.getLabel().compareToIgnoreCase(labelAlternatives.get(j))==0)
                            ok=true;
                    if(ok==false)continue;
                }
                
                    //If the super.method is true, we save the SDFNode and we exit; in case not==false, we'll commit and return true
                    //If the super.method is false, we rollback, i.e., we remove these tags and we try on the next SDFNode, if any.
                if(super.checkConstraints(SDFNodes.get(i), idInstance)==true)upSatisfyingSDFNode=SDFNodes.get(i);
                else rollback();
            }
            
                //if no subsequent SDFNode satisfies the condition, but the "not" is true, it is actually require that any SDFNode satisfies the condition! We return true.
                //Instead, if not==false, we return false: we had to satisfy the condition, but no previous SDFNode(s) does.
                //So, in both cases, the returning value is the value of "not".
            if(upSatisfyingSDFNode==null)
            {
                SDFLogger.endTracing(this, not);
                return not;
            }
            else
            {
                    //If the up-SDFNode satisfies the condition, and "not" is true ... we then return false. But before we have to rollback, as the tags
                    //on upSatisfyingSDFNode are on the top of the stack! Otherwise (if not==false), we return true, but for the same reason, we first have 
                    //to commit, i.e., moving the tags of upSatisfyingSDFNode to the lower level in the stack.
                    //So, in both cases, the returning value is the *NEGATION* of "not"
                if(not==true)rollback();
                else commit();
                SDFLogger.endTracing(this, !not);
                return !not;
            }
        }
        
            //Takes and returns all SDFNode at maxHeight, but the ones included in alreadyUsedSDFNodes
        protected ArrayList<SDFNode> getSDFNodesAtMaxHeight(SDFNode node, int height, Hashtable<SDFNode,SDFNode> SDFNodes2FirstDependent) throws Exception
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
                    
                //These conditions should be never verified (well, only depth==0, to stop the recursion)... but, just in case.
            if((node==null)||(height<=0)) return new ArrayList<SDFNode>();
            
            SDFHead governor = node.SDFHead.getGovernor();
            if(governor==null)return ret;
            
                //The Hashtable SDFHead2SDFNode has been loaded within the SDFTagger, within buildSDFNodesChain ... do you remember? ;-)
                //It contains the mapping between SDFHead(s) and SDFNode(s).
            SDFNode SDFNodeOfGovernor = owner.SDFTagger.SDFHead2SDFNode.get(governor);
            ret.add(SDFNodeOfGovernor);
            SDFNodes2FirstDependent.put(SDFNodeOfGovernor, node);
            ArrayList<SDFNode> subNodes = getSDFNodesAtMaxHeight(SDFNodeOfGovernor, height-1, SDFNodes2FirstDependent);
            for(int j=0; j<subNodes.size(); j++) ret.add(subNodes.get(j));
            
            return ret;
        }
        
            //This has to be defined for maxHeight, not e labelAlternatives
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            while(SDFCode.indexOf("£E")==0)
            {
                labelAlternatives.add(SDFCode.substring(2, SDFCode.indexOf("£E",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£E",2)+2, SDFCode.length());
            }
         
            if(SDFCode.indexOf("£C")==0)
            {
                maxHeight = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
            }
            
            if(maxHeight==-1)
                throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxHeight");

            super.parseSDFCode(SDFCode);
        }
    }
    
        //With Dependent is much more complex!!! We need to check if a *tuple* of <Dependent>(s) satisfies the dependents of an SDFHead.
        //This is enforced by the procedure matchTuplesOfDependents of SDFRule, defined below. Also, the checkConstraints of the class
        //Dependent does not override the one of SDFRule, it has a parameter more and a different return value.
    protected class Dependent extends SDFRule
    {
        protected int maxDepth = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();
        
        public Dependent(String SDFCode, SDFRule owner) throws Exception
        {
            super(null,null,null);//we bypass SDFRule's constructor (read above at "public SDFRule(...) throws Exception" why)
                   
            this.owner = owner;
            this.id = owner.id;
            this.priority = owner.priority;
            this.SDFTagger = owner.SDFTagger;
            this.SDFLogger = owner.SDFLogger;
            this.stack = owner.stack;
            this.SDFNodeConstraintsFactory = owner.SDFNodeConstraintsFactory;
            this.prevsInPrevStarsCurrentlyInExecution = owner.prevsInPrevStarsCurrentlyInExecution;
            this.nextsInNextStarsCurrentlyInExecution = owner.nextsInNextStarsCurrentlyInExecution;
            
                //We store the SDFCode in this attribute; we'll parse it only if/when checkConstraints will called for the first time
            saveSDFCode = SDFCode;
        }
        
            //This method does *NOT* override the one in the superclass, contrary to what it is done in Prev, Next, and Governor.
            //For the Dependent(s), we have the method matchTuplesOfDependents to enforce the multiple (Dependents) condition.
        
            //This method takes in input the SDFNode of the *GOVERNOR* and it finds all its dependents within maxDepth that satisfy the condition of this Dependent.
            //It fills an Hashtable<SDFNode, ArrayList<SDFTag>> that associates the SDFNode of these dependents with the SDFTag(s) found during the recursions.
            //Of course, that means that we always rollback the stacks, once we have registered the SDFTag(s) on its top, returned by recursively satisfying this
            //Dependent, on the Hashtable.
        protected void checkConstraints(SDFNode SDFNodeGovernor, long idInstance, Hashtable<SDFNode, ArrayList<SDFTag>> htDependentSDNodes2Tags)throws Exception
        {   
                //In the constructor we saved the SDFCode, because we parse it only if it is needed.
                //Now, it is needed.
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
            
                //This procedure returns all SDFNode(s), dependents of SDFNodeGovernor, at (maximum) maxDepth depth.
            ArrayList<SDFNode> SDFNodes = getSDFNodesAtMaxDepth(SDFNodeGovernor, maxDepth);
            
                //are there down-SDFNode(s)? Is this perhaps required?
            if(SDFNodes.isEmpty())
            {
                SDFLogger.startTracingOnNoSDFNodes(this);
                
                    //if not=true, we are asking the non-existence of a Dependent. This is indeed met in case the ArrayList<SDFNode> SDFNodes is empty.
                    //Otherwise it is false: we ask the existence of a certain Dependent but there is not any dependent.
                SDFLogger.endTracing(this, not);
                return;
            }

            for(int i=0; i<SDFNodes.size(); i++)
            {
                    //First we check the laber; if it fails, we can avoid rollbacking, because super.checkConstraints has not been done yet. We try the next in SDFNodes
                if(labelAlternatives.isEmpty()==false)
                {
                    boolean ok = false;
                    for(int j=0; (j<labelAlternatives.size())&&(ok==false); j++)
                        if(SDFNodes.get(i).SDFHead.getLabel().compareToIgnoreCase(labelAlternatives.get(j))==0)
                            ok=true;
                    if(ok==false)continue;
                }
                
                    //If the super.method is true, we register the association SDFNode->ArrayList<SDFTag> in the returning hashtable.
                boolean ok = super.checkConstraints(SDFNodes.get(i), idInstance);
                if(ok==true)htDependentSDNodes2Tags.put(SDFNodes.get(i), stack.get(0));
                
                    //If ok=true & not=false dobbiamo registrare "OK" su questo Dependent
                    //If ok=false & not=true dobbiamo registrare "OK" su questo Dependent
                    //In the other two cases (ok=true&not=true oppure ok=false&not=false) we must register "FAILED" on this Dependent.
                    //So, in both cases we register the *XOR* of "ok" and "not".
                SDFLogger.endTracing(this, ok^not);
                                
                    //WE ALWAYS ROLLBACK!!! It is only inside the matchTuplesOfDependents that we'll decide with SDFTag(s) have to be committed.
                rollback();
            }
        }
        
            //Takes and returns all SDFNode at maxDepth
        protected ArrayList<SDFNode> getSDFNodesAtMaxDepth(SDFNode node, int depth) throws Exception
        {
            ArrayList<SDFNode> ret = new ArrayList<SDFNode>();
                    
                //These conditions should be never verified (well, only depth==0, to stop the recursion)... but, just in case.
            if((node==null)||(depth<=0)) return new ArrayList<SDFNode>();
            
            SDFHead[] depsSDFHead = node.SDFHead.getDependents();
            if((depsSDFHead==null)||(depsSDFHead.length==0)) return ret;
            
            for(int i=0; i<depsSDFHead.length; i++)
            {
                    //The Hashtable SDFHead2SDFNode has been loaded within the SDFTagger, within buildSDFNodesChain ... do you remember? ;-)
                    //It contains the mapping between SDFHead(s) and SDFNode(s).
                SDFNode SDFNodeOfDependent = owner.SDFTagger.SDFHead2SDFNode.get(depsSDFHead[i]);
                ret.add(SDFNodeOfDependent);
                
                    //recursion
                ArrayList<SDFNode> subNodes = getSDFNodesAtMaxDepth(SDFNodeOfDependent, depth-1);
                for(int j=0; j<subNodes.size(); j++) ret.add(subNodes.get(j));
            }

            return ret;
        }
        
            //This has to be defined for maxHeight, not e labelAlternatives
        protected void parseSDFCode(String SDFCode) throws Exception
        {
            if(SDFCode.indexOf("£D")==0)
            {
                not = true;
                SDFCode = SDFCode.substring(2, SDFCode.length());
            }
            
            while(SDFCode.indexOf("£E")==0)
            {
                labelAlternatives.add(SDFCode.substring(2, SDFCode.indexOf("£E",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£E",2)+2, SDFCode.length());
            }
            
            if(SDFCode.indexOf("£C")==0)
            {
                maxDepth = Integer.parseInt(SDFCode.substring(2, SDFCode.indexOf("£C",2)));
                SDFCode = SDFCode.substring(SDFCode.indexOf("£C",2)+2, SDFCode.length());
            }
                        
            if(maxDepth==-1) throw new Exception("Exception while I was loading a Constraint on SDFRule with id="+id+"; you must specify maxDepth in <Dependent>");

            super.parseSDFCode(SDFCode);
        }
    }
    
        //This procedure looks for a tuple of SDFHead[] included in SDFNodeGovernor.SDFHead.getDependents() that satisfies the ArrayList<Dependent>.
        //If more than one tuple satisfies the ArrayList<Dependent>, it selects the first that it finds. Once a tuple is selected
        //all tags assigned in the recursions are committed on the stack.
        //Note that ArrayList<Dependent> dependents will be ordered such that all Dependent.not=true are on the bottom of the ArrayList while all Dependent.not=false
        //are on the top. This will facilitate the finding of a tuple.
    
        //This method assumes that all dependents in the ArrayList<Dependent> are ordered such that the ones with not=true are *AT THE END* of the ArrayList.
        //This is enforced within the constructor of convert2SDFCode.java: when we retrieve the ArrayList<Dependent> from the XML, we order it in this sense
        //before further processing its elements.
    protected boolean matchTuplesOfDependents(ArrayList<Dependent> dependents, SDFNode SDFNodeGovernor, long id, long idInstance, long priority) throws Exception
    {
            //For each Dependent in the ArrayList<Dependent>, we calculate a Hashtable<SDFNode, ArrayList<SDFTag>>; the keys of this Hashtable are *ALL* the SDFNode(s)
            //satisfy this Dependent. Each of them is associated with the list of Tag(s) that has been committed while satisfying the SDFNode on the Dependent,
            //possibly via recursions. Of course, no ArrayList<Tag> has been committed on the stack yet.
            //We need to find an assignment of the SDFNode(s) to the Dependent(s) in the ArrayList<Dependent> such that every SDFNode is used *ONLY ONCE* to satisfy
            //a Dependent.not=false and the list of the SDFNode(s) satisfying all Dependent.not=true is *empty* (if Dependent.not=true, no dependent of SDFNodeGovernor
            //must satisfy it; if there is at least one of them who does, this is used to satisfy a (single) Dependent.not=false, so that it is removed from the 
            //list of the dependents that satisfy the Dependent.not=true and this turns out to be empty.

            //We calculate all possibleAssignments (calculated by trying to satisfying each Dependent in ArrayList<Dependent>, with each possible SDFNode within
            //the maxDepth) and we must try to fill the array "assignment", i.e., assigning a single SDFNode from possibleAssignments to every Dependent.not=false 
            //and none to any Dependent.not=true (none means that the possibleAssignments for this Dependent must be the empty set).
            //NB. We first fill dependents2SDFTags, which contains the mapping until the SDFTag(s), then we extract its keys and we create an ArrayList<SDFNode>
            //that we insert in possibleAssignments, then we try to fill the SDFNode[] assignment.
        Hashtable<Dependent, ArrayList<SDFNode>> possibleAssignments = new Hashtable<Dependent, ArrayList<SDFNode>>();
        Hashtable<Dependent, Hashtable<SDFNode, ArrayList<SDFTag>>> dependents2SDFTags = new Hashtable<Dependent, Hashtable<SDFNode, ArrayList<SDFTag>>>();
        SDFNode[] assignments = new SDFNode[dependents.size()];
            //The need for another Hashtable otherOptions will be clarified later. We declare it now because we need 
            //to initialize it, with empty ArrayList<SDFNode>(s), during this for cycle.
        Hashtable<Dependent, ArrayList<SDFNode>> otherOptions = new Hashtable<Dependent, ArrayList<SDFNode>>();
        
        for(int i=0;i<dependents.size();i++)
        {
                //We create and we fill the Hashtable<SDFNode, ArrayList<SDFTag>> on the Dependent at "i"
            Hashtable<SDFNode, ArrayList<SDFTag>> htDependentSDNodes2Tags = new Hashtable<SDFNode, ArrayList<SDFTag>>();
            dependents2SDFTags.put(dependents.get(i), htDependentSDNodes2Tags);
            
            dependents.get(i).checkConstraints(SDFNodeGovernor, idInstance, htDependentSDNodes2Tags);
                    
                //We extract all keys and we create an ArrayList<SDFNode>, that we insert in possibleAssignments
            ArrayList<SDFNode> tempSDFNodes = new ArrayList<SDFNode>();
            Enumeration en = htDependentSDNodes2Tags.keys();
            while(en.hasMoreElements())tempSDFNodes.add((SDFNode)en.nextElement());
            possibleAssignments.put(dependents.get(i), tempSDFNodes);
            otherOptions.put(dependents.get(i), new ArrayList<SDFNode>());
            assignments[i]=null;//we init the assignments with null values
        }
        
        
            //Now we try to find a possible assignment. 
            //For every Dependent, we try all possible combination: we try the first SDFNode from the corresponding ArrayList<SDFNode>.
            //This SDFNode *CANNOT* be reused (this is very important: the assignment is *injective*). If we'll manage to do it, we'll 
            //arrive until the end of the array assignments (by incrementing index below). If it is not possible, we rollback, i.e.,
            //we'll try another assignment. For Dependent.not=true, it is correct if there are no possible assignments, i.e., if the
            //corresponding ArrayList<SDFNode> is empty or it only has SDFNode(s) that cannot be reused.
            //We need to use the otherOptions Hashtable to keep track of the SDFNode(s) that cannot be reused under a certain partial
            //assignment, but that can be reused under another one.
        
            //At every iteraction, we try to find an assignment to assignment[index] or a non-assignment, when Dependent.not=true;
            //that's why the ArrayList<Dependent> has all Dependent(s).not=true at the bottom: we must look for an non-assignment to 
            //them AFTER we remove the possible SDFNode(s) that cannot be reused.
            //index can more both forward (index++) or backward (index--). When it moves backwards it is because a previous assignment
            //is not good, and so we need to try another one. Of course, if move backwards until the beginning, no assignment is good
            //and we need to return false.
        int index = 0;
        while((index>-1)&&(index<dependents.size()))
        {
            ArrayList<SDFNode> validPossibleAssignments = possibleAssignments.get(dependents.get(index));
            ArrayList<SDFNode> invalidPossibleAssignments = otherOptions.get(dependents.get(index));
            
                //We are obliged to init assignmentFound... but I still indicate when assignmentFound is assigned to false 
                //in two of the four branches below, in order to enhance comprehension.
            boolean assignmentFound=false;
            if((dependents.get(index).not==true)&&(validPossibleAssignments.isEmpty()==false)){assignmentFound=false;}
            else if((dependents.get(index).not==true)&&(validPossibleAssignments.isEmpty()==true)){assignmentFound=true;}
            else if((dependents.get(index).not==false)&&(validPossibleAssignments.isEmpty()==true)){assignmentFound=false;}
            else if((dependents.get(index).not==false)&&(validPossibleAssignments.isEmpty()==false))
            {
                    //The fourth case is the one who needs more processing. We make an assignment among the valid ones... and we removed it from 
                    //the available ones! We also remove it from the valid ones for all SDFNode(s) after index (SDFNode(s) can be used only once!).
                assignments[index] = validPossibleAssignments.remove(0);
                invalidPossibleAssignments.add(assignments[index]);
                for(int i=index+1;i<dependents.size();i++)
                    if(possibleAssignments.get(dependents.get(i)).remove(assignments[index])==true)
                        otherOptions.get(dependents.get(i)).add(assignments[index]);
                assignmentFound=true;
            }
            
                //If the assignment has been found, we try to assign the next ones. Otherwise we try another assignment. rollback: we restore
                //the invalid ones at this index, then index--;
            if(assignmentFound==true)index++;
            else
            {
                while(invalidPossibleAssignments.isEmpty()==false)validPossibleAssignments.add(invalidPossibleAssignments.remove(0));
                index--;
            }
        }
        
            //if index=-1, all assignments that we tried didn't work. We must return false. No need to register the failure in the SDFLogger:
            //matchTuplesOfDependents is a method of SDFRule not of the nested subclass Dependent, so the failure will be registered within
            //the method "checkConstraints", from which "matchTuplesOfDependents" is called.
        if(index==-1)return false;
        
            //we commit, i.e., we add all SDFTag(s) of the assigned SDFNode(s) to the ArrayList on the top of the stack, and we return true.
        for(int i=0;i<assignments.length;i++)
        {
                //if it is null, we were requiring the non-existence of a Dependent; therefore assignments[i] was left at null.
                //And, of course, a null SDFNode does not carry any SDFTag...
            if(assignments[i]==null)continue;
            
            ArrayList<SDFTag> SDFTags = dependents2SDFTags.get(dependents.get(i)).get(assignments[i]);
            for(int j=0;j<SDFTags.size();j++)stack.get(0).add(SDFTags.get(j));
        }
        
        return true;
    }
    
/********************************************************************************************************************************************************/
/******************************************************************* parseSDFCode ***********************************************************************/
/********************************************************************************************************************************************************/
    
        //This method is used in SDFRule (and the nested classes Prev, Next, Dependent, and Governor) to fill headAlternatives, endOfSentence, headId, and tags,
        //and to recursively build the subsequent Prev, Next, Dependent, and Governor.
    protected void parseSDFCode(String SDFCode) throws Exception
    {
            //We extract the portion of the SDFCode referring to the SDFNode where we are. Then, we will recursively build the nested classes
        String SDFCodeOfSDFNode = SDFCode;
        
            //We take the index of the closest one between prevStarAlternatives, prevAlternatives, nextStarAlternatives, nextAlternatives, dependentsAlternatives, 
            //and governorAlternatives: that is the index where the recursions start. 
        int index = SDFCode.indexOf("ç");//prevStarAlternatives
        int otherIndex = SDFCode.indexOf("%");//prevAlternatives
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("?");//nextStarAlternatives
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("!");//nextAlternatives
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf(";");//dependentsAlternatives
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        otherIndex = SDFCode.indexOf("&");//governorAlternatives
        if((index==-1)||((otherIndex!=-1)&&(otherIndex<index)))index=otherIndex;
        
            //if index is still -1, SDFCodeOfSDFNode must be SDFCode; otherwise we take the substring from 0 to the first index
        if(index!=-1)
        {
            SDFCodeOfSDFNode = SDFCodeOfSDFNode.substring(0, index);
            SDFCode = SDFCode.substring(index, SDFCode.length());
            
                //BUT! If there is a single '$' in SDFCodeOfSDFNode, it means that the character equal to ç, %, ?, !, ;, or & is in the form or lemma!
                //In such a case, we must extend SDFCodeOfSDFNode until the end if <headAlternatives>, i.e., until we find another $
            if(SDFCodeOfSDFNode.indexOf("$")==SDFCodeOfSDFNode.lastIndexOf("$"))
            {
                SDFCodeOfSDFNode = SDFCodeOfSDFNode+SDFCode.substring(0, SDFCode.indexOf("$")+1);
                SDFCode = SDFCode.substring(SDFCode.indexOf("$")+1, SDFCode.length());
            }
        }
        
            //We fill the attribute SDFNodeConstraints with the Factory. And we load its attribute 
        SDFNodeConstraints = SDFNodeConstraintsFactory.FactorySDFNodeConstraints();
        SDFNodeConstraints.loadAttributes(SDFCodeOfSDFNode, this);
        
            //prevStarAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf("ç")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String prevStarAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(prevStarAlternativesCode.isEmpty()==false)
            {
                counter =  prevStarAlternativesCode.substring(0, prevStarAlternativesCode.indexOf(",")+1);
                String prevCode = prevStarAlternativesCode.substring(counter.length(), prevStarAlternativesCode.lastIndexOf(counter));
                prevStarAlternativesCode = prevStarAlternativesCode.substring(prevStarAlternativesCode.lastIndexOf(counter)+counter.length(), prevStarAlternativesCode.length());
                prevStarAlternatives.add(new Prev(prevCode, owner));
            }
        }

            //prevAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf("%")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String prevAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(prevAlternativesCode.isEmpty()==false)
            {
                counter =  prevAlternativesCode.substring(0, prevAlternativesCode.indexOf(",")+1);
                String prevCode = prevAlternativesCode.substring(counter.length(), prevAlternativesCode.lastIndexOf(counter));
                prevAlternativesCode = prevAlternativesCode.substring(prevAlternativesCode.lastIndexOf(counter)+counter.length(), prevAlternativesCode.length());
                prevAlternatives.add(new Prev(prevCode, owner));
            }
        }

            //nextStarAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf("?")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String nextStarAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(nextStarAlternativesCode.isEmpty()==false)
            {
                counter =  nextStarAlternativesCode.substring(0, nextStarAlternativesCode.indexOf(",")+1);
                String nextCode = nextStarAlternativesCode.substring(counter.length(), nextStarAlternativesCode.lastIndexOf(counter));
                nextStarAlternativesCode = nextStarAlternativesCode.substring(nextStarAlternativesCode.lastIndexOf(counter)+counter.length(), nextStarAlternativesCode.length());
                nextStarAlternatives.add(new Next(nextCode, owner));
            }
        }
        
            //nextAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf("!")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String nextAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(nextAlternativesCode.isEmpty()==false)
            {
                counter =  nextAlternativesCode.substring(0, nextAlternativesCode.indexOf(",")+1);
                String nextCode = nextAlternativesCode.substring(counter.length(), nextAlternativesCode.lastIndexOf(counter));
                nextAlternativesCode = nextAlternativesCode.substring(nextAlternativesCode.lastIndexOf(counter)+counter.length(), nextAlternativesCode.length());
                nextAlternatives.add(new Next(nextCode, owner));
            }
        }
        
            //governorAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf("&")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String governorAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(governorAlternativesCode.isEmpty()==false)
            {
                counter =  governorAlternativesCode.substring(0, governorAlternativesCode.indexOf(",")+1);
                String governorCode = governorAlternativesCode.substring(counter.length(), governorAlternativesCode.lastIndexOf(counter));
                governorAlternativesCode = governorAlternativesCode.substring(governorAlternativesCode.lastIndexOf(counter)+counter.length(), governorAlternativesCode.length());
                governorAlternatives.add(new Governor(governorCode, owner));
            }
        }
        
            //dependentsAlternatives (it is recursive! we can find the last one with the number...)
        if(SDFCode.indexOf(";")==0)
        {
            String counter =  SDFCode.substring(0, SDFCode.indexOf(",")+1);
            String dependentsAlternativesCode = SDFCode.substring(counter.length(), SDFCode.lastIndexOf(counter));
            SDFCode = SDFCode.substring(SDFCode.lastIndexOf(counter)+counter.length(), SDFCode.length());
            
            while(dependentsAlternativesCode.isEmpty()==false)
            {
                ArrayList<Dependent> dependents = new ArrayList<Dependent>();
                dependentsAlternatives.add(dependents);
                
                counter =  dependentsAlternativesCode.substring(0, dependentsAlternativesCode.indexOf(",")+1);
                String dependentsCode = dependentsAlternativesCode.substring(counter.length(), dependentsAlternativesCode.lastIndexOf(counter));
                dependentsAlternativesCode = dependentsAlternativesCode.substring(dependentsAlternativesCode.lastIndexOf(counter)+counter.length(), dependentsAlternativesCode.length());
                
                while(dependentsCode.isEmpty()==false)
                {
                    counter =  dependentsCode.substring(0, dependentsCode.indexOf(",")+1);
                    String dependentCode = dependentsCode.substring(counter.length(), dependentsCode.lastIndexOf(counter));
                    dependentsCode = dependentsCode.substring(dependentsCode.lastIndexOf(counter)+counter.length(), dependentsCode.length());
                    dependents.add(new Dependent(dependentCode, owner));
                }
            }
        }
    }
    
/********************************************************************************************************************************************************/
/********************************************************** buildXMLcode (needed by SDFLogger) **********************************************************/
/********************************************************************************************************************************************************/
    
    public Element buildSDFRuleXML()throws Exception
    {
        try
        {
                //if some branch was not executed, its saveSDFCode is still full (it is its SDFNodeConstraints that is null). We build the attributes before 
                //generating the SDFRule (we want the SDFRule as a whole in the SDFDebug file, even the parts that were not executed).
            if(saveSDFCode!=null)parseSDFCode(saveSDFCode);
            saveSDFCode=null;
        }
        catch(Exception e)
        {
            throw new Exception("Error in generating the <SDFDebug> XML Element");
        }
            
        String className = this.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        Element SDFRuleStep = new Element(className.toLowerCase());
        
        if(className.compareToIgnoreCase("sdfrule")==0)
        {
            SDFRuleStep = new Element("SDFRule");//I prefer this capitalized...
            SDFRuleStep.setAttribute("id", ""+id);
            SDFRuleStep.setAttribute("priority", ""+priority);
        }
        else
        {
            if((this instanceof Prev)&&((Prev)this).not==true)SDFRuleStep=new Element("notPrev");
            else if((this instanceof Next)&&((Next)this).not==true)SDFRuleStep=new Element("notNext");
            else if((this instanceof Governor)&&((Governor)this).not==true)SDFRuleStep=new Element("notGovernor");
            else if((this instanceof Dependent)&&((Dependent)this).not==true)SDFRuleStep=new Element("notDependent");
            if(this instanceof Prev)SDFRuleStep.setAttribute("maxDistance", ""+((Prev)this).maxDistance);
            else if(this instanceof Next)SDFRuleStep.setAttribute("maxDistance", ""+((Next)this).maxDistance);
            else if(this instanceof Governor)SDFRuleStep.setAttribute("maxHeight", ""+((Governor)this).maxHeight);
            else if(this instanceof Dependent)SDFRuleStep.setAttribute("maxDepth", ""+((Dependent)this).maxDepth);
        }
        
        Element[] tagsAndHeadAlternatives = SDFNodeConstraints.buildTagsAndHeadAlternativesXML();
        for(int i=0;i<tagsAndHeadAlternatives.length;i++)SDFRuleStep.getContent().add(tagsAndHeadAlternatives[i]);
        
        if(prevStarAlternatives.size()>0)
        {
            Element prevStarAlternativesE = new Element("prevStarAlternatives");
            SDFRuleStep.getContent().add(prevStarAlternativesE);
            for(int i=0;i<this.prevStarAlternatives.size();i++)prevStarAlternativesE.getContent().add(this.prevStarAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(prevAlternatives.size()>0)
        {
            Element prevAlternativesE = new Element("prevAlternatives");
            SDFRuleStep.getContent().add(prevAlternativesE);
            for(int i=0;i<this.prevAlternatives.size();i++)prevAlternativesE.getContent().add(this.prevAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(nextStarAlternatives.size()>0)
        {
            Element nextStarAlternativesE = new Element("nextStarAlternatives");
            SDFRuleStep.getContent().add(nextStarAlternativesE);
            for(int i=0;i<this.nextStarAlternatives.size();i++)nextStarAlternativesE.getContent().add(this.nextStarAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(nextAlternatives.size()>0)
        {
            Element nextAlternativesE = new Element("nextAlternatives");
            SDFRuleStep.getContent().add(nextAlternativesE);
            for(int i=0;i<this.nextAlternatives.size();i++)nextAlternativesE.getContent().add(this.nextAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(governorAlternatives.size()>0)
        {
            Element governorAlternativesE = new Element("governorAlternatives");
            SDFRuleStep.getContent().add(governorAlternativesE);
            for(int i=0;i<this.governorAlternatives.size();i++)governorAlternativesE.getContent().add(this.governorAlternatives.get(i).buildSDFRuleXML());
        }
        
        if(dependentsAlternatives.size()>0)
        {
            Element dependentsAlternativesE = new Element("dependentsAlternatives");
            SDFRuleStep.getContent().add(dependentsAlternativesE);
            
            for(int j=0;j<this.dependentsAlternatives.size();j++)
            {
                Element dependents = new Element("dependents");
                dependentsAlternativesE.getContent().add(dependents);
                for(int i=0;i<this.dependentsAlternatives.get(j).size();i++)dependents.getContent().add(this.dependentsAlternatives.get(j).get(i).buildSDFRuleXML());
            }
        }
        
        return SDFRuleStep;
    }
}
