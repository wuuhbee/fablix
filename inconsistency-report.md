## Inconsistency Report:
* actors63.xml: 
    * **2525 inconsistencies**
    * Some *birth years* tags were empty or had non-numeric values (e.g. "n.a.", "*", or "1915+") similar to movie years, so both were considered null. Birth years are not required in our schema, so they can be null and were inserted as such.
*  movies243.xml: 
    * **40 inconsistencies**
    Some *fid*, *title*, *cat*, and *director* tags were empty/null or the years of some movies were not completely numeric (e.g. 199x, or 19yy, empty, or null). Our schema requires that *fids*, *titles*, *directors*, and *years* are not null, so since they failed to fit the format of the schema, they were labeled as inconsistencies. For null/empty *cat* tags, the genre was skipped but the movie was still inserted.
* cast93.xml: 
    * **1137 inconsistencies**
    * Some movies listed the *actor's name* as "s a" which refers to "some actor". Since the name isn't specified, those rows were skipped, since users won't typically look for an "other" actor. The schema required that the stars name be not null, so this was labeled as an inconsistency since it could not be parsed into a plausible name.