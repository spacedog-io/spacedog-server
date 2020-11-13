# ElasticSearch plugins

This version of SpaceDog embeds ElasticSearch and needs the very specific repository-s3 plugin.

If you need another version of the plugin jar than the one contained in this `plugins` folder

- download the  `elasticsearch-x.y.z.tar.gz`  you need
- extract the archive into  `elasticsearch-x.y.z`  
- execute `elasticsearch-x.y.z/bin/elasticsearch-plugin install repository-s3`
- copy `elasticsearch-x.y.z/plugins/repository-s3/repository-s3-x.y.z.jar` to the `server-v2/plugins` folder
- update the `install-repository-s3-plugin.sh` with the right ElasticSearch version
- install the plugin jar in maven with the `install-repository-s3-plugin.sh`

