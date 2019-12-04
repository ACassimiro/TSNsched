#!/usr/bin/env bash
# install dependencies for TSNSched (on Ubunutu)

#define remote repo for z3 libary
z3Libary="https://github.com/Z3Prover/z3.git"

#emojis
UNICORN="\360\237\246\204"
DIR="\360\237\222\276"
DOWNLOAD="\342\254\207"
PYTHON="\360\237\220\215"
PARTY="\360\237\216\211"
UPDATE="\342\231\273"
CHECK="\342\234\205"
HAMMER="\356\204\226"

JAVACUR=-1
JAVAREQ=8

checkForCommand () {
    if command -v $1 >/dev/null 2>&1 ; then
        printf "${CHECK} $1 already installed.\n"
    else
        echo >&2 "$1 required"
        # install package
        confirm "Install $1 with sudo? [y/N]" && sudo apt-get install $1 
    fi
}

checkForPackage () {
    if [ $(dpkg-query -W -f='${Status}' $1 2>/dev/null | grep -c "ok installed") -eq 0 ]; then
        #install package
        confirm "Install $1 with sudo?[y/N]" && sudo apt-get install $1
#        printf "${CHECK} $1 already installed.\n"
    fi
}

installJDK8() {
    printf "Installing openjdk-8-jdk. "
    confirm && apt-get install "openjdk-8-jdk"
}

checkJavaVersion(){
    #check if any java is present
    if command -v "java" >/dev/null 2>&1 ; then
        # is the correct jdk installed?
        printf "Java Version: "
        JAVACUR=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
        printf "${JAVACUR}\n"
            if [ "$JAVACUR" -ne "$JAVAREQ" ];then
                installJDK8
            fi
    else
        installJDK8    
    fi

}

openLogs(){
    gedit ./* &>/dev/null &
}

spinner(){
    (while :; do for c in / - \\ \|; do printf '%s\b' "$c"; sleep 1; done; done) &
}

confirm() {
    # call with a prompt string or use a default
    read -r -p "${1:-Are you sure? [y/N]} " response
    case "$response" in
        [yY][eE][sS]|[yY]) 
            true
            ;;
        *)
            false
            ;;
    esac
}

#require script to run as root
if [ "$EUID" -ne 0 ]
  then printf "Please rerun with sudo.\n sudo $0.\n"
  exit
fi


printf "${UNICORN} Installing dependencies...\n"

printf "${DIR} Current directory: "
pwd
printf "${DIR} Switching directory to "
cd ..
pwd

# install z3libary if folder doesn't exit
if [ ! -d "z3" ]; then
    printf "${DOWNLOAD} Cloning z3 libary. "
    confirm && git clone ${z3Libary}
fi

printf "${DIR} Switching directory to: "
cd z3
pwd

checkForCommand "python3"

#check for python3-distutils
if python3 -c "import distutils.sysconfig" &> /dev/null; then
    printf "${CHECK} python3-distutils already installed.\n"
else
    confirm "Install python3-distutils with sudo? [y/N]" && apt-get install python3-distutils
fi

# install make and g++
checkForPackage "build-essential"

# install proper java version
checkJavaVersion


#run mk_make script
if [ ! -d "build" ]; then
    printf "${PYTHON} Run Python make Script. Building Z3 using make and GCC\n"
    confirm && python3 scripts/mk_make.py --java
    printf "${HAMMER} Typed cd build; make to build Z3\n"
    cd build; make
    printf "    sudo make install...\n    ${HAMMER}"
    sudo make install
    cd ..
fi

printf "${PARTY} Installed all dependencies succesfully.\n"
printf "${DIR} Current directory: "
pwd
cd ..
printf "${DIR} Current directory: "
pwd
cd TSNsched/Script/
printf "${DIR} Current directory: "
pwd

#create TSNSched examples
confirm "Run TSNSched example file now? [y/N]" && printf "Creating TSNSched example...\n" && spinner && sh ./generateSchedule.sh example.java && { printf '\n'; kill $! && wait $!; } 2>/dev/null



cd output
printf "${CHECK} TSNSched example generated.\n"
confirm "Open logs now? [y/N]" && openLogs

printf "${PARTY} End of script\n"
