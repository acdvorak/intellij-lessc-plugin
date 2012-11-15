/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asual.lesscss;

import com.asual.lesscss.loader.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rostislav Hristov
 * @author Uriah Carpenter
 * @author Noah Sloan
 */
public class LessEngine {

    private static LessEngine instance;
	
	private final Log logger = LogFactory.getLog(getClass());
	
	private final LessOptions options;
	private final ResourceLoader loader;
	
	private Scriptable scope;
	private Function compile;

    public static synchronized LessEngine getInstance() throws LessException {
        if (instance == null)
            instance = new LessEngine();
        return instance;
    }
	
	public LessEngine() throws LessException {
		this(new LessOptions());
	}
	
	public LessEngine(LessOptions options) throws LessException {
		this(options, defaultResourceLoader(options));
	}

	private static ResourceLoader defaultResourceLoader(LessOptions options) {
		ResourceLoader resourceLoader = new ChainedResourceLoader(
				new FilesystemResourceLoader(), new ClasspathResourceLoader(
						LessEngine.class.getClassLoader()),
				new HTTPResourceLoader());
		if(options.isCss()) {
			return new CssProcessingResourceLoader(resourceLoader);			
		} 
		resourceLoader = new UnixNewlinesResourceLoader(resourceLoader);
		return resourceLoader;
	}

    /**
     *
     * @param options
     * @param loader
     * @see <a href="http://www.envjs.com/doc/guides#running-embed">Embedding EnvJS</a>
     */
	public LessEngine(LessOptions options, ResourceLoader loader) throws LessException {
		this.options = options;
		this.loader = loader;
		try {
			logger.debug("Initializing LESS Engine.");

			ClassLoader classLoader = getClass().getClassLoader();

			URL less = options.getLess();
			URL env = classLoader.getResource("env.js");
			URL envjs = classLoader.getResource("env.rhino.1.2.js");
			URL engine = classLoader.getResource("engine.js");
			URL cssmin = classLoader.getResource("cssmin.js");

			Context cx = Context.enter();

			cx.setOptimizationLevel(9);
//            cx.setOptimizationLevel(-1);

            cx.setLanguageVersion(Context.VERSION_1_5);

			logger.debug("Using implementation version: " + cx.getImplementationVersion());

//            Global global = Main.getGlobal();
			Global global = new Global();

			global.init(cx);

			scope = cx.initStandardObjects(global);

			cx.evaluateReader(scope, new InputStreamReader(env.openConnection().getInputStream()), envjs.getFile(), 1, null);
			cx.evaluateReader(scope, new InputStreamReader(env.openConnection().getInputStream()), env.getFile(), 1, null);
			Scriptable lessEnv = (Scriptable) scope.get("lessenv", scope);
			lessEnv.put("charset", lessEnv, options.getCharset());
			lessEnv.put("css", lessEnv, options.isCss());
			lessEnv.put("loader", lessEnv, Context.javaToJS(loader, scope));
			cx.evaluateReader(scope, new InputStreamReader(less.openConnection().getInputStream()), less.getFile(), 1, null);
			cx.evaluateReader(scope, new InputStreamReader(cssmin.openConnection().getInputStream()), cssmin.getFile(), 1, null);
			cx.evaluateReader(scope, new InputStreamReader(engine.openConnection().getInputStream()), engine.getFile(), 1, null);
			compile = (Function) scope.get("compile", scope);
		} catch (Exception e) {
			logger.error("LESS Engine initialization failed.", e);
            throw new LessException(e);
		} finally {
            Context.exit();
        }
	}
	
	public String compile(String inputLessCode) throws LessException {
		return compile(inputLessCode, null, false);
	}
	
	public String compile(String inputLessCode, String location) throws LessException {
		return compile(inputLessCode, location, false);
	}
	
	public String compile(String inputLessCode, String location, boolean compress) throws LessException {
		try {
			long time = System.currentTimeMillis();
			String result = call(compile, new Object[] { inputLessCode, location == null ? "" : location, compress });
			logger.debug("The compilation of '" + inputLessCode + "' took " + (System.currentTimeMillis () - time) + " ms.");
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public String compile(URL inputLessFile) throws LessException {
		return compile(inputLessFile, false);
	}
	
	public String compile(URL inputLessFile, boolean compress) throws LessException {
		try {
			long time = System.currentTimeMillis();
			String location = inputLessFile.toString();
			logger.debug("Compiling URL: " + location);
			String source = loader.load(location, options.getCharset());
			String result = call(compile, new Object[] {source, location, compress});
			logger.debug("The compilation of '" + inputLessFile + "' took " + (System.currentTimeMillis () - time) + " ms.");
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public String compile(File inputLessFile) throws LessException {
		return compile(inputLessFile, false);
	}
	
	public String compile(File inputLessFile, boolean compress) throws LessException {
		try {
			long time = System.currentTimeMillis();
			String location = inputLessFile.getAbsolutePath();
			logger.debug("Compiling File: " + "file:" + location);
			String source = loader.load(location, options.getCharset());
			String result = call(compile, new Object[] {source, location, compress});
			logger.debug("The compilation of '" + inputLessFile + "' took " + (System.currentTimeMillis () - time) + " ms.");
			return result;
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}
	
	public void compile(File inputLessFile, File output) throws LessException, IOException {
		compile(inputLessFile, output, false);
	}
	
	public void compile(File inputLessFile, File output, boolean compress) throws LessException, IOException {
		try {
			String content = compile(inputLessFile, compress);
			if (!output.exists()) {
				output.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write(content);
			bw.close();
		} catch (Exception e) {
			throw parseLessException(e);
		}
	}

	private synchronized String call(Function fn, Object[] args) {
		return (String) Context.call(null, fn, scope, scope, args);
	}
	
	private LessException parseLessException(Exception root) throws LessException {
		logger.debug("Parsing LESS Exception", root);
		if (root instanceof JavaScriptException) {
			Scriptable value = (Scriptable) ((JavaScriptException) root).getValue();
			String type = ScriptableObject.getProperty(value, "type").toString() + " Error";
			String message = ScriptableObject.getProperty(value, "message").toString();
			String filename = "";
			if (ScriptableObject.getProperty(value, "filename") != null) {
				filename = ScriptableObject.getProperty(value, "filename").toString();
			}
			int line = -1;
			if (ScriptableObject.getProperty(value, "line") != null) {
				line = ((Double) ScriptableObject.getProperty(value, "line")).intValue();
			}
			int column = -1;
			if (ScriptableObject.getProperty(value, "column") != null) {
				column = ((Double) ScriptableObject.getProperty(value, "column")).intValue();
			}
			List<String> extractList = new ArrayList<String>();
			if (ScriptableObject.getProperty(value, "extract") != null) {
				NativeArray extract = (NativeArray) ScriptableObject.getProperty(value, "extract");
				for (int i = 0; i < extract.getLength(); i++) {
					if (extract.get(i, extract) instanceof String) {
						extractList.add(((String) extract.get(i, extract)).replace("\t", " "));
					}
				}
			}
			throw new LessException(message, type, filename, line, column, extractList);
		}
		throw new LessException(root);
	}
	
}