package org.drools.compiler.xpath.tobeinstrumented;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import org.drools.compiler.xpath.tobeinstrumented.model.Child;
import org.drools.compiler.xpath.tobeinstrumented.model.Man;
import org.drools.compiler.xpath.tobeinstrumented.model.PojoWithCollections;
import org.drools.compiler.xpath.tobeinstrumented.model.School;
import org.drools.compiler.xpath.tobeinstrumented.model.TMDirectory;
import org.drools.compiler.xpath.tobeinstrumented.model.TMFile;
import org.drools.compiler.xpath.tobeinstrumented.model.TMFileSet;
import org.drools.compiler.xpath.tobeinstrumented.model.Toy;
import org.drools.compiler.xpath.tobeinstrumented.model.Woman;
import org.drools.core.base.ClassObjectType;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.phreak.ReactiveCollection;
import org.drools.core.phreak.ReactiveList;
import org.drools.core.phreak.ReactiveObject;
import org.drools.core.phreak.ReactiveSet;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftInputAdapterNode;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.ReactiveFromNode;
import org.drools.core.reteoo.TupleMemory;
import org.drools.core.util.Iterator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.kie.maven.plugin.BytecodeInjectReactive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class XPathTest {
    private static final Logger LOG = LoggerFactory.getLogger(XPathTest.class);
    
    @BeforeClass
    public static void init() throws Exception {
        ClassPool cp = new ClassPool(null);
        cp.appendSystemPath();
        cp.appendClassPath(BytecodeInjectReactive.classpathFromClass(ReactiveObject.class));
        
        BytecodeInjectReactive enhancer = BytecodeInjectReactive.newInstance(cp);
        
        /*
           BYTECODE LOADING WARNING: in the following, ensure class is passed as canonical String representation,
           and NOT as a Clazz.class.getCanonicalName(). This is because yes technically it would be possible to
           classload the new instrumented and bytecode injected class in a separate classloader
           HOWEVER it would just make more trouble more down during actual testing.
           This is because if here a class is identified as Clazz.class.getCanonicalName()
           it would be classloaded and remain loaded as the original class,
           hence later in the test there is no way to reload the same class as the bytecode instrumented version
           which is the ultimate intention behind of these tests.
         */
        byte[] personBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.Person");
        byte[] schoolBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.School");
        byte[] childBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.Child");
        byte[] tmfileBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.TMFile");
        byte[] tmfilesetBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.TMFileSet");
        byte[] tmdirectoryBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.TMDirectory");
        byte[] pojoWithCollectionsBytecode = enhancer.injectReactive("org.drools.compiler.xpath.tobeinstrumented.model.PojoWithCollections");
        
        ClassPool cp2 = new ClassPool(null);
        cp2.appendSystemPath();
        cp2.appendClassPath(BytecodeInjectReactive.classpathFromClass(ReactiveObject.class));
        
        loadClassAndUtils(cp2, personBytecode);
        loadClassAndUtils(cp2, childBytecode);
        loadClassAndUtils(cp2, schoolBytecode);
        loadClassAndUtils(cp2, tmfileBytecode);
        loadClassAndUtils(cp2, tmfilesetBytecode); 
        loadClassAndUtils(cp2, tmdirectoryBytecode);
        loadClassAndUtils(cp2, pojoWithCollectionsBytecode);
    }
    
    private static void loadClassAndUtils(ClassPool cp, byte[] bytecode) throws Exception {
        CtClass theCtClass = cp.makeClass(new ByteArrayInputStream(bytecode));
        Class<?> class1 = theCtClass.toClass();
        
        LOG.info("Bytecode-injected class for {} now having the following methods:", theCtClass.getName());
        for ( Method m : class1.getMethods() ) {
            LOG.info(" {}", m );
        }
        
        File dir = new File("./target/JAVASSIST/");
        dir.mkdirs();
        // please note it is INTENTIONAL to write the file with package name part of the file itself, for easier browsing
        // anyway the directory is NOT intended for classloading, but just for browsing bytecode for decompilation.
        File bytecodeFile = new File(dir, theCtClass.getPackageName() + theCtClass.getName() + ".class" );
        bytecodeFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(bytecodeFile);
        fos.write(bytecode);
        fos.close();
        LOG.info("Written bytecode for {} in file: {}.", theCtClass.getName(), bytecodeFile);
    }
    
    @Test
    public void testPojoWithCollectionsBytecode() {
        PojoWithCollections pojo = new PojoWithCollections(new ArrayList(), new ArrayList(), new HashSet());
        
        LOG.info("testPojoWithCollectionsBytecode(): {}", pojo);
        
        assertEquals(pojo.getFieldCollection().getClass(), ReactiveCollection.class);
        assertEquals(pojo.getFieldList().getClass(), ReactiveList.class);
        assertEquals(pojo.getFieldSet().getClass(), ReactiveSet.class);
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveOnLia() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  Man( $toy: /wife/children[age > 10]/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 10 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 2, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );

        list.clear();
        debbie.setAge( 11 );
        ksession.fireAllRules();

        assertEquals( 1, list.size() );
        assertTrue( list.contains( "doll" ) );
    }

    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveDeleteOnLia() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  Man( $toy: /wife/children[age > 10]/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieBase kbase = new KieHelper().addContent( drl, ResourceType.DRL ).build();
        KieSession ksession = kbase.newKieSession();
        
        EntryPointNode epn = ( (InternalKnowledgeBase) ksession.getKieBase() ).getRete().getEntryPointNodes().values().iterator().next();
        ObjectTypeNode otn = epn.getObjectTypeNodes().values().stream()
                .filter(ot-> ot.getObjectType() instanceof ClassObjectType && !((ClassObjectType)ot.getObjectType()).getClassName().contains("InitialFact"))
                .findFirst().get();
        LeftInputAdapterNode lian = (LeftInputAdapterNode)otn.getObjectSinkPropagator().getSinks()[0];
        ReactiveFromNode from1 = (ReactiveFromNode)lian.getSinkPropagator().getSinks()[0];
        ReactiveFromNode from2 = (ReactiveFromNode)from1.getSinkPropagator().getSinks()[0];
        ReactiveFromNode from3 = (ReactiveFromNode)from2.getSinkPropagator().getSinks()[0];

        BetaMemory betaMemory = ( (InternalWorkingMemory) ksession ).getNodeMemory(from3).getBetaMemory();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 11 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 3, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );
        assertTrue( list.contains( "doll" ) );

        TupleMemory tupleMemory = betaMemory.getLeftTupleMemory();
        assertEquals( 2, betaMemory.getLeftTupleMemory().size() );
        Iterator<LeftTuple> it = tupleMemory.iterator();
        for ( LeftTuple next = it.next(); next != null; next = it.next() ) {
            Object obj = next.getFactHandle().getObject();
            assertTrue( obj == charlie || obj == debbie );
        }

        list.clear();
        debbie.setAge( 10 );
        ksession.fireAllRules();

        assertEquals( 0, list.size() );

        assertEquals( 1, betaMemory.getLeftTupleMemory().size() );
        it = tupleMemory.iterator();
        for ( LeftTuple next = it.next(); next != null; next = it.next() ) {
            Object obj = next.getFactHandle().getObject();
            assertTrue( obj == charlie );
        }
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveOnBeta() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  $i : Integer()\n" +
                "  Man( $toy: /wife/children[age > $i]?/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 10 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( 10 );
        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 2, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );

        list.clear();
        debbie.setAge( 11 );
        ksession.fireAllRules();

        assertEquals( 1, list.size() );
        assertTrue( list.contains( "doll" ) );
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactive2Rules() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List toyList\n" +
                "global java.util.List teenagers\n" +
                "\n" +
                "rule R1 when\n" +
                "  $i : Integer()\n" +
                "  Man( $toy: /wife/children[age >= $i]/toys )\n" +
                "then\n" +
                "  toyList.add( $toy.getName() );\n" +
                "end\n" +
                "rule R2 when\n" +
                "  School( $child: /children[age >= 13] )\n" +
                "then\n" +
                "  teenagers.add( $child.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> toyList = new ArrayList<String>();
        ksession.setGlobal( "toyList", toyList );
        List<String> teenagers = new ArrayList<String>();
        ksession.setGlobal( "teenagers", teenagers );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 15 );
        Child debbie = new Child( "Debbie", 12 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        School school = new School( "Da Vinci" );
        school.addChild( charlie );
        school.addChild( debbie );

        ksession.insert( 13 );
        ksession.insert( bob );
        ksession.insert( school );
        ksession.fireAllRules();

        assertEquals( 2, toyList.size() );
        assertTrue( toyList.contains( "car" ) );
        assertTrue( toyList.contains( "ball" ) );

        assertEquals( 1, teenagers.size() );
        assertTrue( teenagers.contains( "Charles" ) );

        toyList.clear();
        debbie.setAge( 13 );
        ksession.fireAllRules();

        assertEquals( 1, toyList.size() );
        assertTrue( toyList.contains( "doll" ) );

        assertEquals( 2, teenagers.size() );
        assertTrue( teenagers.contains( "Charles" ) );
        assertTrue( teenagers.contains( "Debbie" ) );
    }
    
    /**
     * Copied from drools-compiler ( fixed with DROOLS-1302 ) 
     */
    @Test
    public void testListReactive() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "\n" +
                "rule R2 when\n" +
                "  School( $child: /children[age >= 13 && age < 20] )\n" +
                "then\n" +
                "  System.out.println( $child );\n" +
                "  insertLogical( $child );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();
        
        
        Child charlie = new Child( "Charles", 15 );
        Child debbie = new Child( "Debbie", 19 );
        School school = new School( "Da Vinci" );
        school.addChild( charlie );
        ksession.insert( school );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
        
        school.addChild( debbie );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertTrue(ksession.getObjects().contains(debbie));
        
        school.removeChild(debbie);
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
        
        school.addChild( debbie );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertTrue(ksession.getObjects().contains(debbie));
        
        debbie.setAge( 20 );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
    }
    
    private List<?> factsCollection(KieSession ksession) {
        List<Object> res = new ArrayList<>();
        res.addAll(ksession.getObjects());
        return res;
    }
    
    /**
     * Copied from drools-compiler
     */
    @Test
    public void testMiscSetMethods() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "\n" +
                "rule R2 when\n" +
                "  TMFileSet( $id: name, $p: /files[size >= 100] )\n" +
                "then\n" +
                "  System.out.println( $id + \".\" + $p.getName() );\n" +
                "  insertLogical(      $id + \".\" + $p.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();
        
        TMFileSet x = new TMFileSet("X");
        TMFileSet y = new TMFileSet("Y");
        ksession.insert( x );
        ksession.insert( y );
        ksession.fireAllRules();
        assertFalse (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        
        TMFile file0 = new TMFile("File0", 47);
        TMFile file1 = new TMFile("File1", 47);
        TMFile file2 = new TMFile("File2", 47);
        x.getFiles().add(file2);
        x.getFiles().addAll(Arrays.asList(new TMFile[]{file0, file1}));
        y.getFiles().add(file2);
        y.getFiles().add(file0);
        y.getFiles().add(file1);
        ksession.fireAllRules();
        assertFalse (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertFalse (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));

        file0.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));
        
        y.getFiles().remove( file1 ); // removing File1 from Y
        file1.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertTrue  (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));
        
        file2.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertTrue  (factsCollection(ksession).contains("X.File1"));
        assertTrue  (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertTrue  (factsCollection(ksession).contains("Y.File2"));
    }
    
    /**
     * Copied from drools-compiler
     */
    @Test
    public void testMiscListMethods() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "\n" +
                "rule R2 when\n" +
                "  TMDirectory( $id: name, $p: /files[size >= 100] )\n" +
                "then\n" +
                "  System.out.println( $id + \".\" + $p.getName() );\n" +
                "  insertLogical(      $id + \".\" + $p.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();
        
        TMDirectory x = new TMDirectory("X");
        TMDirectory y = new TMDirectory("Y");
        ksession.insert( x );
        ksession.insert( y );
        ksession.fireAllRules();
        assertFalse (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        
        TMFile file0 = new TMFile("File0", 47);
        TMFile file1 = new TMFile("File1", 47);
        TMFile file2 = new TMFile("File2", 47);
        x.getFiles().add(file2);
        x.getFiles().addAll(0, Arrays.asList(new TMFile[]{file0, file1}));
        y.getFiles().add(0, file2);
        y.getFiles().add(0, file0);
        y.getFiles().add(1, file1);
        ksession.fireAllRules();
        assertFalse (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertFalse (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));

        file0.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertFalse (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));
        
        y.getFiles().remove(1); // removing File1 from Y
        file1.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertTrue  (factsCollection(ksession).contains("X.File1"));
        assertFalse (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertFalse (factsCollection(ksession).contains("Y.File2"));
        
        file2.setSize( 999 );        
        ksession.fireAllRules();
        assertTrue  (factsCollection(ksession).contains("X.File0"));
        assertTrue  (factsCollection(ksession).contains("X.File1"));
        assertTrue  (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertTrue  (factsCollection(ksession).contains("Y.File2"));
        
        TMFile file0R = new TMFile("File0R", 999);
        x.getFiles().set(0, file0R);
        ksession.fireAllRules();
        assertFalse (factsCollection(ksession).contains("X.File0"));
        assertTrue  (factsCollection(ksession).contains("X.File0R"));
        assertTrue  (factsCollection(ksession).contains("X.File1"));
        assertTrue  (factsCollection(ksession).contains("X.File2"));
        assertTrue  (factsCollection(ksession).contains("Y.File0"));
        assertFalse (factsCollection(ksession).contains("Y.File1"));
        assertTrue  (factsCollection(ksession).contains("Y.File2"));
    }
}
