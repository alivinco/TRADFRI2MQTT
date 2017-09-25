import sys

def set_deb_control(version , arch):
    file_name  = "./debian/DEBIAN/control"
    template = "Package: ikea-ad\n"
    template+= "Version: "+version+"\n"
    template+= "Replaces: ikea-ad\n"
    template+= "Architecture: all\n"
    template+= "Maintainer: Aleksandrs Livincovs <aleksandrs.livincovs@gmail.com>\n"
    template+= "Description: Ikea Tradgri - FIMP adapter .\n"

    f = open(file_name,"w")
    f.write(template)
    f.close()


if __name__ == "__main__":
   environment = sys.argv[1] 
   version = sys.argv[2]
   arch = sys.argv[3]
   set_deb_control(version,arch)
