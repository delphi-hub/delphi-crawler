package de.upb.cs.swt.delphi.crawler.storage

import com.sksamuel.elastic4s.mappings.FieldDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._

object ElasticFeatureListMapping {

  //Returns a Seq object of FieldDefinitions that defines all fields in the feature mapping
  def getMapAsSeq: Seq[FieldDefinition] =
    featureMap.toSeq.map(tup => tup._2(tup._1))

  //Stores the mapping for the features returned by Hermes as a Map of feature names to field types
  //These field names are slightly different from the feature names Hermes returns
  //  in that any surrounding double quotes are stripped and any newlines are replaced with spaces
  /*TODO: Change mapping types to minimize search times, and confirm that the results returned by Hermes do not exceed
    TODO:      the maximum values of each of these field types*/
  private val featureMap: Map[String, String => FieldDefinition] = Map[String, String => FieldDefinition](
    "ProjectPackages" -> intField,
    "⟨SizeOfInheritanceTree⟩" -> doubleField,
    "ProjectFields" -> intField,
    "LibraryMethods" -> intField,
    "ProjectInstructions" -> intField,
    "ProjectMethods" -> intField,
    "LibraryClassFiles" -> intField,
    "LibraryFields" -> intField,
    "LibraryPackages" -> intField,
    "ProjectClassFiles" -> intField,
    "0 FPC" -> intField,
    "1-3 FPC" -> intField,
    "4-10 FPC" -> intField,
    ">10 FPC" -> intField,
    "0 MPC" -> intField,
    "1-3 MPC" -> intField,
    "4-10 MPC" -> intField,
    ">10 MPC" -> intField,
    "1-3 CPP" -> intField,
    "4-10 CPP" -> intField,
    ">10 CPP" -> intField,
    "0 NOC" -> intField,
    "1-3 NOC" -> intField,
    "4-10 NOC" -> intField,
    ">10 NOC" -> intField,
    "linear methods (McCabe)" -> intField,
    "2-3 McCabe" -> intField,
    "4-10 McCabe" -> intField,
    ">10 McCabe" -> intField,
    "Designator" -> intField,
    "Taxonomy" -> intField,
    "Joiner" -> intField,
    "Pool" -> intField,
    "Function Pointer" -> intField,
    "Function Object" -> intField,
    "Cobol Like" -> intField,
    "Stateless" -> intField,
    "Common State" -> intField,
    "Immutable" -> intField,
    "Restricted Creation" -> intField,
    "Sampler" -> intField,
    "Box" -> intField,
    "Compound Box" -> intField,
    "Canopy" -> intField,
    "Record" -> intField,
    "Data Manager" -> intField,
    "Sink" -> intField,
    "Outline" -> intField,
    "Trait" -> intField,
    "State Machine" -> intField,
    "Pure Type" -> intField,
    "Augmented Type" -> intField,
    "Pseudo Class" -> intField,
    "Implementor" -> intField,
    "Overrider" -> intField,
    "Extender" -> intField,
    "unused private fields" -> intField,
    "unused package visible fields" -> intField,
    "unused protected fields" -> intField,
    "unused public fields" -> intField,
    "package visible fields only used by defining type" -> intField,
    "protected fields only used by defining type" -> intField,
    "public fields only used by defininig type " -> intField,
    "Trivial Class.forName Usage" -> intField,
    "Nontrivial Class.forName Usage" -> intField,
    "nop (opcode:0)" -> intField,
    "aconst_null (opcode:1)" -> intField,
    "iconst_m1 (opcode:2)" -> intField,
    "iconst_0 (opcode:3)" -> intField,
    "iconst_1 (opcode:4)" -> intField,
    "iconst_2 (opcode:5)" -> intField,
    "iconst_3 (opcode:6)" -> intField,
    "iconst_4 (opcode:7)" -> intField,
    "iconst_5 (opcode:8)" -> intField,
    "lconst_0 (opcode:9)" -> intField,
    "lconst_1 (opcode:10)" -> intField,
    "fconst_0 (opcode:11)" -> intField,
    "fconst_1 (opcode:12)" -> intField,
    "fconst_2 (opcode:13)" -> intField,
    "dconst_0 (opcode:14)" -> intField,
    "dconst_1 (opcode:15)" -> intField,
    "bipush (opcode:16)" -> intField,
    "sipush (opcode:17)" -> intField,
    "ldc (opcode:18)" -> intField,
    "ldc_w (opcode:19)" -> intField,
    "ldc2_w (opcode:20)" -> intField,
    "iload (opcode:21)" -> intField,
    "lload (opcode:22)" -> intField,
    "fload (opcode:23)" -> intField,
    "dload (opcode:24)" -> intField,
    "aload (opcode:25)" -> intField,
    "iload_0 (opcode:26)" -> intField,
    "iload_1 (opcode:27)" -> intField,
    "iload_2 (opcode:28)" -> intField,
    "iload_3 (opcode:29)" -> intField,
    "lload_0 (opcode:30)" -> intField,
    "lload_1 (opcode:31)" -> intField,
    "lload_2 (opcode:32)" -> intField,
    "lload_3 (opcode:33)" -> intField,
    "fload_0 (opcode:34)" -> intField,
    "fload_1 (opcode:35)" -> intField,
    "fload_2 (opcode:36)" -> intField,
    "fload_3 (opcode:37)" -> intField,
    "dload_0 (opcode:38)" -> intField,
    "dload_1 (opcode:39)" -> intField,
    "dload_2 (opcode:40)" -> intField,
    "dload_3 (opcode:41)" -> intField,
    "aload_0 (opcode:42)" -> intField,
    "aload_1 (opcode:43)" -> intField,
    "aload_2 (opcode:44)" -> intField,
    "aload_3 (opcode:45)" -> intField,
    "iaload (opcode:46)" -> intField,
    "laload (opcode:47)" -> intField,
    "faload (opcode:48)" -> intField,
    "daload (opcode:49)" -> intField,
    "aaload (opcode:50)" -> intField,
    "baload (opcode:51)" -> intField,
    "caload (opcode:52)" -> intField,
    "saload (opcode:53)" -> intField,
    "istore (opcode:54)" -> intField,
    "lstore (opcode:55)" -> intField,
    "fstore (opcode:56)" -> intField,
    "dstore (opcode:57)" -> intField,
    "astore (opcode:58)" -> intField,
    "istore_0 (opcode:59)" -> intField,
    "istore_1 (opcode:60)" -> intField,
    "istore_2 (opcode:61)" -> intField,
    "istore_3 (opcode:62)" -> intField,
    "lstore_0 (opcode:63)" -> intField,
    "lstore_1 (opcode:64)" -> intField,
    "lstore_2 (opcode:65)" -> intField,
    "lstore_3 (opcode:66)" -> intField,
    "fstore_0 (opcode:67)" -> intField,
    "fstore_1 (opcode:68)" -> intField,
    "fstore_2 (opcode:69)" -> intField,
    "fstore_3 (opcode:70)" -> intField,
    "dstore_0 (opcode:71)" -> intField,
    "dstore_1 (opcode:72)" -> intField,
    "dstore_2 (opcode:73)" -> intField,
    "dstore_3 (opcode:74)" -> intField,
    "astore_0 (opcode:75)" -> intField,
    "astore_1 (opcode:76)" -> intField,
    "astore_2 (opcode:77)" -> intField,
    "astore_3 (opcode:78)" -> intField,
    "iastore (opcode:79)" -> intField,
    "lastore (opcode:80)" -> intField,
    "fastore (opcode:81)" -> intField,
    "dastore (opcode:82)" -> intField,
    "aastore (opcode:83)" -> intField,
    "bastore (opcode:84)" -> intField,
    "castore (opcode:85)" -> intField,
    "sastore (opcode:86)" -> intField,
    "pop (opcode:87)" -> intField,
    "pop2 (opcode:88)" -> intField,
    "dup (opcode:89)" -> intField,
    "dup_x1 (opcode:90)" -> intField,
    "dup_x2 (opcode:91)" -> intField,
    "dup2 (opcode:92)" -> intField,
    "dup2_x1 (opcode:93)" -> intField,
    "dup2_x2 (opcode:94)" -> intField,
    "swap (opcode:95)" -> intField,
    "iadd (opcode:96)" -> intField,
    "ladd (opcode:97)" -> intField,
    "fadd (opcode:98)" -> intField,
    "dadd (opcode:99)" -> intField,
    "isub (opcode:100)" -> intField,
    "lsub (opcode:101)" -> intField,
    "fsub (opcode:102)" -> intField,
    "dsub (opcode:103)" -> intField,
    "imul (opcode:104)" -> intField,
    "lmul (opcode:105)" -> intField,
    "fmul (opcode:106)" -> intField,
    "dmul (opcode:107)" -> intField,
    "idiv (opcode:108)" -> intField,
    "ldiv (opcode:109)" -> intField,
    "fdiv (opcode:110)" -> intField,
    "ddiv (opcode:111)" -> intField,
    "irem (opcode:112)" -> intField,
    "lrem (opcode:113)" -> intField,
    "frem (opcode:114)" -> intField,
    "drem (opcode:115)" -> intField,
    "ineg (opcode:116)" -> intField,
    "lneg (opcode:117)" -> intField,
    "fneg (opcode:118)" -> intField,
    "dneg (opcode:119)" -> intField,
    "ishl (opcode:120)" -> intField,
    "lshl (opcode:121)" -> intField,
    "ishr (opcode:122)" -> intField,
    "lshr (opcode:123)" -> intField,
    "iushr (opcode:124)" -> intField,
    "lushr (opcode:125)" -> intField,
    "iand (opcode:126)" -> intField,
    "land (opcode:127)" -> intField,
    "ior (opcode:128)" -> intField,
    "lor (opcode:129)" -> intField,
    "ixor (opcode:130)" -> intField,
    "lxor (opcode:131)" -> intField,
    "iinc (opcode:132)" -> intField,
    "i2l (opcode:133)" -> intField,
    "i2f (opcode:134)" -> intField,
    "i2d (opcode:135)" -> intField,
    "l2i (opcode:136)" -> intField,
    "l2f (opcode:137)" -> intField,
    "l2d (opcode:138)" -> intField,
    "f2i (opcode:139)" -> intField,
    "f2l (opcode:140)" -> intField,
    "f2d (opcode:141)" -> intField,
    "d2i (opcode:142)" -> intField,
    "d2l (opcode:143)" -> intField,
    "d2f (opcode:144)" -> intField,
    "i2b (opcode:145)" -> intField,
    "i2c (opcode:146)" -> intField,
    "i2s (opcode:147)" -> intField,
    "lcmp (opcode:148)" -> intField,
    "fcmpl (opcode:149)" -> intField,
    "fcmpg (opcode:150)" -> intField,
    "dcmpl (opcode:151)" -> intField,
    "dcmpg (opcode:152)" -> intField,
    "ifeq (opcode:153)" -> intField,
    "ifne (opcode:154)" -> intField,
    "iflt (opcode:155)" -> intField,
    "ifge (opcode:156)" -> intField,
    "ifgt (opcode:157)" -> intField,
    "ifle (opcode:158)" -> intField,
    "if_icmpeq (opcode:159)" -> intField,
    "if_icmpne (opcode:160)" -> intField,
    "if_icmplt (opcode:161)" -> intField,
    "if_icmpge (opcode:162)" -> intField,
    "if_icmpgt (opcode:163)" -> intField,
    "if_icmple (opcode:164)" -> intField,
    "if_acmpeq (opcode:165)" -> intField,
    "if_acmpne (opcode:166)" -> intField,
    "goto (opcode:167)" -> intField,
    "jsr (opcode:168)" -> intField,
    "ret (opcode:169)" -> intField,
    "tableswitch (opcode:170)" -> intField,
    "lookupswitch (opcode:171)" -> intField,
    "ireturn (opcode:172)" -> intField,
    "lreturn (opcode:173)" -> intField,
    "freturn (opcode:174)" -> intField,
    "dreturn (opcode:175)" -> intField,
    "areturn (opcode:176)" -> intField,
    "return (opcode:177)" -> intField,
    "getstatic (opcode:178)" -> intField,
    "putstatic (opcode:179)" -> intField,
    "getfield (opcode:180)" -> intField,
    "putfield (opcode:181)" -> intField,
    "invokevirtual (opcode:182)" -> intField,
    "invokespecial (opcode:183)" -> intField,
    "invokestatic (opcode:184)" -> intField,
    "invokeinterface (opcode:185)" -> intField,
    "invokedynamic (opcode:186)" -> intField,
    "new (opcode:187)" -> intField,
    "newarray (opcode:188)" -> intField,
    "anewarray (opcode:189)" -> intField,
    "arraylength (opcode:190)" -> intField,
    "athrow (opcode:191)" -> intField,
    "checkcast (opcode:192)" -> intField,
    "instanceof (opcode:193)" -> intField,
    "monitorenter (opcode:194)" -> intField,
    "monitorexit (opcode:195)" -> intField,
    "wide (opcode:196)" -> intField,
    "multianewarray (opcode:197)" -> intField,
    "ifnull (opcode:198)" -> intField,
    "ifnonnull (opcode:199)" -> intField,
    "goto_w (opcode:200)" -> intField,
    "jsr_w (opcode:201)" -> intField,
    "Self-recursive Data Structure" -> intField,
    "Mutually-recursive Data Structure 2 Types" -> intField,
    "Mutually-recursive Data Structure 3 Types" -> intField,
    "Mutually-recursive Data Structure 4 Types" -> intField,
    "Mutually-recursive Data Structure more than 4 Types" -> intField,
    "Never Returns Normally" -> intField,
    "Method with Infinite Loop" -> intField,
    "Class File With Source Attribute" -> intField,
    "Method With Line Number Table" -> intField,
    "Method With Local Variable Table" -> intField,
    "Method With Local Variable Type Table" -> intField,
    "FanOut - Category 1" -> intField,
    "FanOut - Category 2" -> intField,
    "FanOut - Category 3" -> intField,
    "FanOut - Category 4" -> intField,
    "FanOut - Category 5" -> intField,
    "FanOut - Category 6" -> intField,
    "FanIn - Category 1" -> intField,
    "FanIn - Category 2" -> intField,
    "FanIn - Category 3" -> intField,
    "FanIn - Category 4" -> intField,
    "FanIn - Category 5" -> intField,
    "FanIn - Category 6" -> intField,
    "FanIn/FanOut - Category 1" -> intField,
    "FanIn/FanOut - Category 2" -> intField,
    "FanIn/FanOut - Category 3" -> intField,
    "FanIn/FanOut - Category 4" -> intField,
    "FanIn/FanOut - Category 5" -> intField,
    "FanIn/FanOut - Category 6" -> intField,
    "custom ClassLoader implementation" -> intField,
    "Retrieving the SystemClassLoader" -> intField,
    "Retrieving some ClassLoader" -> intField,
    "define new classes/packages" -> intField,
    "accessing resources" -> intField,
    "javax.crypto.Cipher getInstance" -> intField,
    "using SecureRandom" -> intField,
    "using MessageDigest" -> intField,
    "using Signature" -> intField,
    "using Mac" -> intField,
    "cryptographic key handling" -> intField,
    "using KeyStore" -> intField,
    "using Certificates" -> intField,
    "native methods" -> intField,
    "synthetic methods" -> intField,
    "bridge methods" -> intField,
    "synchronized methods" -> intField,
    "varargs methods" -> intField,
    "static initializers" -> intField,
    "static methods (not including static initializers)" -> intField,
    "constructors" -> intField,
    "instance methods" -> intField,
    "java.lang.Class forName" -> intField,
    "reflective instance creation" -> intField,
    "reflective field write" -> intField,
    "reflective field read" -> intField,
    "makes fields accessible" -> intField,
    "makes methods or constructors accessible" -> intField,
    "makes an AccessibleObject accessible (exact type unknown)" -> intField,
    "java.lang.reflect.Method Object invoke(Object, Object[])" -> intField,
    "java.lang.invoke.MethodHandles lookup" -> intField,
    "java.lang.invoke.MethodHandles publicLookup" -> intField,
    "method handle invocation" -> intField,
    "java.lang.reflect.Proxy newProxyInstance" -> intField,
    "Process" -> intField,
    "JVM exit" -> intField,
    "Native Libraries" -> intField,
    "java.lang.System getSecurityManager" -> intField,
    "java.lang.System setSecurityManager" -> intField,
    "Environment" -> intField,
    "Sound" -> intField,
    "Network sockets" -> intField,
    "Object-based Thread Notification" -> intField,
    "Usage of Thread API" -> intField,
    "Usage of ThreadGroup API" -> intField,
    "sun.misc.Unsafe sun.misc.Unsafe getUnsafe()" -> intField,
    "Unsafe - Alloc" -> intField,
    "Unsafe - Array" -> intField,
    "Unsafe - compareAndSwap" -> intField,
    "Unsafe - Class" -> intField,
    "Unsafe - Fence" -> intField,
    "Unsafe - Fetch & Add" -> intField,
    "Unsafe - Heap" -> intField,
    "Unsafe - Heap Get" -> intField,
    "Unsafe - Heap Put" -> intField,
    "Misc" -> intField,
    "Unsafe - Monitor" -> intField,
    "Unsafe - Off-Heap" -> intField,
    "Unsafe - Offset" -> intField,
    "Unsafe - Ordered Put" -> intField,
    "Unsafe - Park" -> intField,
    "Unsafe - Throw" -> intField,
    "Unsafe - Volatile Get" -> intField,
    "Unsafe - Volatile Put" -> intField,
    "java.sql.DriverManager getConnection" -> intField,
    "java.sql.Connection rollback" -> intField,
    "creation and execution of java.sql.Statement" -> intField,
    "creation and execution of java.sql.PreparedStatement" -> intField,
    "creation and execution of java.sql.CallableStatement" -> intField,
    "class file retransformation" -> intField,
    "instrumenting native methods" -> intField,
    "appending class loader search" -> intField,
    "retrieve classes information" -> intField,
    "(concrete) classes" -> intField,
    "abstract classes" -> intField,
    "annotations" -> intField,
    "enumerations" -> intField,
    "marker interfaces" -> intField,
    "simple functional interfaces (single abstract method (SAM) interface)" -> intField,
    "non-functional interface with default methods (Java >8)" -> intField,
    "non-functional interface with static methods (Java >8)" -> intField,
    "(standard) interface" -> intField,
    "module (Java >9)" -> intField,
    "Very Small Inheritance Tree" -> intField,
    "Small Inheritance Tree" -> intField,
    "Medium Inheritance Tree" -> intField,
    "Large Inheritance Tree" -> intField,
    "Very Large Inheritance Tree" -> intField,
    "Huge Inheritance Tree" -> intField,
    "Size of the Inheritance Tree Unknown" -> intField,
    "Class File JDK 1.1 (JDK 1.0.2)" -> intField,
    "Class File Java 5" -> intField,
    "Class File Java 6" -> intField,
    "Class File Java 7" -> intField,
    "Class File Java 8" -> intField,
    "Class File Java 9" -> intField
  )
}