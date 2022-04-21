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


> layer.name=OSM => layer name
> tile.width=256 => Tile X size of original TMS service
> tile.height=256 => Tile Y size of original TMS service
> tile.origin.x=-20037508.34 => Origin coordinates of tile service (tile index x : 0)
> tile.origin.y=20037508.34 => Origin coordinates of tile service (tile index y : 0)
> maxresolution=78271.516953125 => max resolution (width(height) of real coordinates / image width(height))
> zoomlevel=20 => zoom level of Original tms service
> service.start.level=0 => The number at which the zoom level starts
> tile.crs.code=epsg:900913 => CRS of original tms service
> extent=-20037508.34,-20037508.34,20037508.34,20037508.34 => extent of original tms service
> blank.image.url => If the tile does not exist, alternative image URL
> path.generator=com.mango.tms.DefaultPathGenerator => Change if not normal tile numbering rules
> url.pattern=https://tile.openstreetmap.org/%LEVEL%/%COL%/%ROW%.png => Tile service URL pattern. [%LEVEL%,%COL%,%ROW%] It is replaced by the system.
> url.y_order=TB => How to increase the y-index of a tile. TB : TOP to BOTTOM, BT : BOTTOM to TOP
> outline=true => Each tile is marked with a red border and the tile index number is indicated.
> tile.cache=false => Set to 'true' if the tile will be saved to the server folder.
> cache.path=/Your/server/tms/tile/osm
