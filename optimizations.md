## **Optimization Methods**

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
