
#!/usr/bin/env bash

if [ "$(uname)" == "Darwin" ]; then
    which -s brew
	if [[ $? != 0 ]] ; then
	    # Install Homebrew
	    ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
	else
	    brew update
	fi     
	npm install node
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    wget -qO- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash
    nvm install -g npm
fi




git clone --depth 1 --branch v20210826 https://github.com/DeepPhe/DeepPhe-Viz.git

#start the api/webserver
(export PORT=3001 && cd DeepPhe-Viz/api && npm install && npm start)

