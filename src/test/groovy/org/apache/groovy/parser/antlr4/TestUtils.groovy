package org.apache.groovy.parser.antlr4

import groovy.util.logging.Log
import org.apache.groovy.parser.AbstractParser
import org.apache.groovy.parser.Antlr2Parser
import org.apache.groovy.parser.Antlr4Parser
import org.apache.groovy.parser.antlr4.util.ASTComparatorCategory
import org.apache.groovy.parser.antlr4.util.GroovySourceGenerator
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.syntax.Token

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Utilities for test
 *
 * @author  <a href="mailto:realbluesun@hotmail.com">Daniel.Sun</a>
 * @date    2016/09/21
 */

@Log
class TestUtils {
    public static final String DEFAULT_RESOURCES_PATH = 'subprojects/groovy-antlr4-grammar/src/test/resources';
    public static final String RESOURCES_PATH = new File(DEFAULT_RESOURCES_PATH).exists() ? DEFAULT_RESOURCES_PATH : 'src/test/resources';

    static doTest(String path) {
        return doTest(path, ASTComparatorCategory.DEFAULT_CONFIGURATION)
    }

    static doTest(String path, List ignoreClazzList) {
        return doTest(path, addIgnore(ignoreClazzList, ASTComparatorCategory.LOCATION_IGNORE_LIST))
    }

    static doTest(String path, conf) {
        AbstractParser antlr4Parser = new Antlr4Parser()
        AbstractParser antlr2Parser = new Antlr2Parser()

        File file = new File("$RESOURCES_PATH/$path");
        def (newAST, newElapsedTime) = profile { antlr4Parser.parse(file) }
        def (oldAST, oldElapsedTime) = profile { antlr2Parser.parse(file) }


        assertAST(newAST, oldAST, conf);

        long diffInMillis = newElapsedTime - oldElapsedTime;

        if (diffInMillis >= 500) {
            log.warning "${path}\t\t\t\t\tdiff:${diffInMillis / 1000}s,\tnew:${newElapsedTime / 1000}s,\told:${oldElapsedTime / 1000}s."
        }

        return [newAST, oldAST]
    }

    /*
    static unzipAndTest(String path, String entryName) {
        unzipAndTest(path, entryName, ASTComparatorCategory.DEFAULT_CONFIGURATION)
    }
    */

    /*
    static unzipAndTest(String path, String entryName, List ignoreClazzList) {
        unzipAndTest(path, entryName, addIgnore(ignoreClazzList, ASTComparatorCategory.LOCATION_IGNORE_LIST))
    }
    */

    static unzipAndTest(String path, String entryName, conf, Map<String, String> replacementsMap=[:]) {
        AbstractParser antlr4Parser = new Antlr4Parser()
        AbstractParser antlr2Parser = new Antlr2Parser()

        String name = "$path!$entryName";
        String text = readZipEntry(path, entryName);

        replacementsMap?.each {k, v ->
            text = text.replace(k, v);
        }

        def (newAST, newElapsedTime) = profile { antlr4Parser.parse(name, text) }
        def (oldAST, oldElapsedTime) = profile { antlr2Parser.parse(name, text) }


        assertAST(newAST, oldAST, conf);

        long diffInMillis = newElapsedTime - oldElapsedTime;

        if (diffInMillis >= 500) {
            log.warning "${path}!${entryName}\t\t\t\t\tdiff:${diffInMillis / 1000}s,\tnew:${newElapsedTime / 1000}s,\told:${oldElapsedTime / 1000}s."
        }
    }


    static shouldFail(String path, boolean toCheckNewParserOnly = false) {
        shouldFail(path, ASTComparatorCategory.DEFAULT_CONFIGURATION, toCheckNewParserOnly)
    }

    static shouldFail(String path, List ignoreClazzList, boolean toCheckNewParserOnly = false) {
        shouldFail(path, addIgnore(ignoreClazzList, ASTComparatorCategory.LOCATION_IGNORE_LIST), toCheckNewParserOnly)
    }

    static shouldFail(String path, conf, boolean toCheckNewParserOnly = false) {
        AbstractParser antlr4Parser = new Antlr4Parser()
        AbstractParser antlr2Parser = new Antlr2Parser()

        File file = new File("$RESOURCES_PATH/$path");
        def (newAST, newElapsedTime) = profile { antlr4Parser.parse(file) }
        def (oldAST, oldElapsedTime) = profile { antlr2Parser.parse(file) }

        if (toCheckNewParserOnly) {
            assert (newAST == null || newAST.context.errorCollector.hasErrors())
        } else {
            assert (newAST == null || newAST.context.errorCollector.hasErrors()) &&
                    (oldAST == null || oldAST.context.errorCollector.hasErrors())
        }

        long diffInMillis = newElapsedTime - oldElapsedTime;

        if (diffInMillis >= 500) {
            log.warning "${path}\t\t\t\t\tdiff:${diffInMillis / 1000}s,\tnew:${newElapsedTime / 1000}s,\told:${oldElapsedTime / 1000}s."
        }
    }

    static unzipAndFail(String path, String entryName, conf, Map<String, String> replacementsMap=[:], boolean toCheckNewParserOnly = false) {
        AbstractParser antlr4Parser = new Antlr4Parser()
        AbstractParser antlr2Parser = new Antlr2Parser()

        String name = "$path!$entryName";
        String text = readZipEntry(path, entryName);

        replacementsMap?.each {k, v ->
            text = text.replace(k, v);
        }

        def (newAST, newElapsedTime) = profile { antlr4Parser.parse(name, text) }
        def (oldAST, oldElapsedTime) = profile { antlr2Parser.parse(name, text) }

        if (toCheckNewParserOnly) {
            assert (newAST == null || newAST.context.errorCollector.hasErrors())
        } else {
            assert (newAST == null || newAST.context.errorCollector.hasErrors()) &&
                    (oldAST == null || oldAST.context.errorCollector.hasErrors())
        }

        long diffInMillis = newElapsedTime - oldElapsedTime;

        if (diffInMillis >= 500) {
            log.warning "${path}!${entryName}\t\t\t\t\tdiff:${diffInMillis / 1000}s,\tnew:${newElapsedTime / 1000}s,\told:${oldElapsedTime / 1000}s."
        }
    }


    static assertAST(ast1, ast2, conf) {
        assert null != ast1 && null != ast2

        ASTComparatorCategory.apply(conf) {
            assert ast1 == ast2
        }

        assert genSrc(ast1) == genSrc(ast2)
    }

    static genSrc(ModuleNode ast) {
        return new GroovySourceGenerator(ast).gen();
    }

    static profile(Closure c) {
        long begin = System.currentTimeMillis()
        def result = c.call()
        long end = System.currentTimeMillis()

        return [result, end - begin];
    }

    static addIgnore(Class aClass, ArrayList<String> ignore, Map<Class, List<String>> c = null) {
        c = c ?: ASTComparatorCategory.DEFAULT_CONFIGURATION.clone() as Map<Class, List<String>>;
        c[aClass].addAll(ignore)
        return c
    }

    static addIgnore(Collection<Class> aClass, ArrayList<String> ignore, Map<Class, List<String>> c = null) {
        c = c ?: ASTComparatorCategory.DEFAULT_CONFIGURATION.clone() as Map<Class, List<String>>;
        aClass.each { c[it].addAll(ignore) }
        return c
    }

    static readZipEntry(String path, String entryName) {
        String result = "";

        def zf = new ZipFile(new File(path));
        try {
            def is = new BufferedInputStream(zf.getInputStream(new ZipEntry(entryName)));
            result = is.getText("UTF-8");
        } catch (Exception e) {
            log.severe(e.message);
        } finally {
            zf.close();
        }

        return result;
    }

    static doRunAndTest(String path) {
        assert executeScript(path);
    }

    static executeScript(String path) {
        executeScript(createAntlr4Shell(), "$RESOURCES_PATH/$path")
    }

    static executeScript(gsh, String path) {
        def file = new File(path);
        def content = file.text;

        try {
            gsh.evaluate(content);
//            log.info("Evaluated $file")
            return true;
        } catch (Throwable t) {
            log.severe("Failed $file: ${t.getMessage()}");
            return false;
        }
    }

    static createAntlr4Shell() {
        CompilerConfiguration configuration = new CompilerConfiguration(CompilerConfiguration.DEFAULT)
        configuration.pluginFactory = new Antlr4PluginFactory()

        return new GroovyShell(configuration);
    }

    public static final List COMMON_IGNORE_CLASS_LIST = Collections.unmodifiableList([AssertStatement, BreakStatement, ConstructorNode, ExpressionStatement, FieldNode, ForStatement, GenericsType, IfStatement, MethodNode, Parameter, PropertyNode, ReturnStatement, ThrowStatement, Token, WhileStatement]);
}
