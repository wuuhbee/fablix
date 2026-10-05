# 2026-spring-cs-122b-flicks-the-sql

Project 1
* Demo Link: https://youtu.be/ICM0JNFY8to
* Code Freeze Link: https://github.com/uci-jherold2-2026fall-cs122b/2026-spring-cs-122b-flicks-the-sql/tree/project1CodeFreeze

Project 2
* Demo Link: https://youtu.be/mhRgMGeF0lI
* Code Freeze Link: https://github.com/uci-jherold2-2026fall-cs122b/2026-spring-cs-122b-flicks-the-sql/tree/project2CodeFreeze

Project 3
* Demo Link: https://youtu.be/rG1tlhyjIsk
* Code Freeze Link: https://github.com/uci-jherold2-2026fall-cs122b/2026-spring-cs-122b-flicks-the-sql/tree/project3CodeFreeze

## Contributions:
Melyn Lim:
* Project 1: Single Star, Single Movie Webservlet, half the CREATE TABLE queries
* Project 2: Login, Sorting, Browse, Payment, Confirmation, Extend Project 1 pages
* Project 3: Encrypted Passwords, Employee Login, Employee Dashboard, stored-procedure.sql, parse actor and cast XML files, Batch Inserts

Kelly Le:
* Project 1: Movie List, Single Movie .js/.html, half the CREATE TABLE queries
* Project 2: Search, Pagination N and Prev Next, Jump to Prev Page, Shopping Cart, Checkout Buttons
* Project 3: reCaptcha, Employee Dashboard +AddMovie, stored-procedure.sql parse movies XML file, Multithreading, Batch Inserts

## Prepared Statements
Files using `PreparedStatements`:
* [ActorDOMParser.java](src/ActorDOMParser.java)
* [CastDOMParser.java](src/CastDOMParser.java)
* [EmployeeLoginServlet.java](src/EmployeeLoginServlet.java)
* [GenreBrowseServlet.java](src/GenreBrowseServlet.java)
* [GetGenresServlet.java](src/GetGenresServlet.java)
* [GetCartServlet.java](src/GetCartServlet.java)
* [LoginServlet.java](src/LoginServlet.java)
* [MovieDOMParser.java](src/MovieDOMParser.java)
* [MovieListServlet.java](src/MovieListServlet.java)
* [PaymentServlet.java](src/PaymentServlet.java)
* [MovieListServlet.java](src/MovieListServlet.java)
* [SearchServlet.java](src/SearchServlet.java)
* [ShoppingCartServlet.java](src/ShoppingCartServlet.java)
* [SingleMovieServlet.java](src/SingleMovieServlet.java)
* [SingleStarServlet.java](src/SingleStarServlet.java)
* [TitleBrowseServlet.java](src/TitleBrowseServlet.java)

## Optimization Methods

### Multithreading: 
We used multithreading in the "Parse XML" stage of the ETL Pipeline to simultaneously run the parsers for actors63.xml and movies243.xml each in their own thread. We didn't use threading for cast93.xml because it's inserts relied on the other two files completing their inserts into the table first.
* Before: 1037ms actors63.xml + 2540ms movies243.xml + 3666ms cast93.xml
* After: 6902ms (from actors63.xml + movies243.xml + cast93.xml)
### Batch Inserts: 
Since there were thousands of insert statements for each file, we decided to use batch inserts in the "Execute SQL" stage to prevent having multiple single INSERT queries each making their own call to MySQL. Batch inserts will reduce the number of calls to the network, acting like a single query made of multiple INSERT statements.
* movies243.xml
  * Before: 7345ms
  * After: 2540ms
* actors63.xml
  * Before: 1759ms
  * After: 755ms
* cast93.xml
  * Before: 11060ms
  * After: 3666ms
 
## Inconsistency Report:
* actors63.xml: 
    * **2525 inconsistencies**
    * Some *birth years* tags were empty or had non-numeric values (e.g. "n.a.", "*", or "1915+") similar to movie years, so both were considered null. Birth years are not required in our schema, so they can be null and were inserted as such.
*  movies243.xml: 
    * **40 inconsistencies**
    * Some *film id* tags were empty or the *years* of some movies were not completely numeric (e.g. 199x, or 19yy).Our schema requires that movie ids and years to not be null, so since they failed to be fit the format of the schema, they were labeled as inconsistencies.
* cast93.xml: 
    * **1137 inconsistencies**
    * Some movies listed the *actor's name* as "s a" which refers to "some actor". Since the name isn't specified, those rows were skipped, since users won't typically look for an "other" actor. The schema required that the stars name be not null, so this was labeled as an inconsistency since it could not be parsed into a plausible name.
