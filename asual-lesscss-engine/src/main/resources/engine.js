/*
 * Copyright 2012 Andrew C. Dvorak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

print = lessenv.print;
quit = lessenv.quit;
readFile = lessenv.readFile;
delete arguments;

var basePath = function(path) {
	if (path != null) {
		var i = path.lastIndexOf('/');
		if(i > 0) {
			return path.substr(0, i + 1);
		}
	}
	return "";
};

less.Parser.importer = function(path, paths, fn) {
	if (!/^\//.test(path) && !/^\w+:/.test(path)) {
		path = paths[0] + path;
	}
	if (path != null) {
		new(less.Parser)({ optimization: 3, paths: [basePath(path)] }).parse(String(lessenv.loader.load(path, lessenv.charset)), function (e, root) {
			if (e instanceof Object)
				throw e;
			fn(e, root);
			if (e instanceof Object)
				throw e;
		});
	}
};

var compile = function(source, location, compress) {
	var result;
	new (less.Parser) ({ optimization: 3, paths: [basePath(location)] }).parse(source, function (e, root) {
		if (e instanceof Object)
			throw e;
		result = root.toCSS();
		if (compress)
			result = exports.compressor.cssmin(result);
		if (e instanceof Object)
			throw e;
	});
	return result;
};

