# gs-tms2wms
> Install
> 1. copy gs-tms2wms-2.19.0.jar and paste to geoserver/WEB-INF/lib folder
> 2. restart Geoserver
> 3. add new DataStore
> 4. Select TMS service under Raster Datastore
> 5. select xxx.properties file and save
> 6. publish tms layer
> end


.properties file

smple file is OpenStreetMap Tms service


> <b>layer.name=OSM</b> => layer name
> 
> <b>tile.width=256</b> => Tile X size of original TMS service
> 
> <b>tile.height=256</b> => Tile Y size of original TMS service
> 
> <b>tile.origin.x=-20037508.34</b> => Origin coordinates of tile service (tile index x : 0)
> 
> <b>tile.origin.y=20037508.34</b> => Origin coordinates of tile service (tile index y : 0)
> 
> <b>maxresolution=78271.516953125</b> => max resolution (width(height) of real coordinates / image width(height))
> 
> <b>zoomlevel=20</b> => zoom level of Original tms service
> 
> <b>service.start.level=0</b> => The number at which the zoom level starts
> 
> <b>tile.crs.code=epsg:900913</b> => CRS of original tms service
> 
> <b>extent=-20037508.34,-20037508.34,20037508.34,20037508.34</b> => extent of original tms service
> 
> <b>blank.image.url=http://your.blank.img/blank.png</b> => If the tile does not exist, alternative image URL
> 
> <b>path.generator=com.mango.tms.DefaultPathGenerator</b> => Change if not normal tile numbering rules
> 
> <b>url.pattern=https://tile.openstreetmap.org/%LEVEL%/%COL%/%ROW%.png</b> => Tile service URL pattern. [%LEVEL%,%COL%,%ROW%] It is replaced by the system.
> 
> <b>url.y_order=TB</b> => How to increase the y-index of a tile. TB : TOP to BOTTOM, BT : BOTTOM to TOP
> 
> <b>outline=true</b> => Each tile is marked with a red border and the tile index number is indicated.
> 
> <b>tile.cache=false</b> => Set to 'true' if the tile will be saved to the server folder.
> 
> <b>cache.path=/Your/server/tile/directory/osm</b>
> 
