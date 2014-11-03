sql-schema-comparer-eclipse-plugin
==================================

**Experimental** [eclipse] plug-in for the [sql-schema-comparer library][ssc]

[eclipse]: http://eclipse.org/
[ssc]: https://github.com/hschink/sql-schema-comparer

## Details ##

`sql-schema-comparer-eclipse-plugin` integrates [sql-schema-comparer][ssc] supporting the following functions:

- Check of simple SQL (`SELECT`) statements
- Check of simple JPA entities

Near future versions of the plug-in will include [sql-schema-comparer][ssc]'s database schema comparison support.

## Installation ##

Currently, there exists no update site. You may install the plug-in manually or you start it by importing the plug-in project into eclipse.

### Manual installation ###

Clone the repo. Import the repo using _Existing Projects into Workspace_. [Export a plug-in JAR](http://help.eclipse.org/luna/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Fexport_wizards%2Fexport_plugins.htm) (which is actually a ZIP file). Extract the plug-in ZIP to ``ECLIPSE_ROOT/dropins``. You should find the plug-in JAR at ``ECLIPSE_ROOT/dropins/plugins/``. Restart eclipse with optino ``-clean``.

## Usage ##

You need to activate the plug-in by pressing _SQL Schema Comparer_ in the project's context menu.

## License ##

The plug-in is published under the terms of the [Eclipse Public License (EPL) 1.0][epl].

[epl]: http://www.eclipse.org/legal/epl-v10.html
