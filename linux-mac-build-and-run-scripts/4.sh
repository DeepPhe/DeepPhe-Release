git clone --depth 1 --branch v20210826 https://github.com/DeepPhe/DeepPhe-Viz.git

#start the api/webserver
(export PORT=3001 && cd DeepPhe-Viz/api && npm install && npm start)

