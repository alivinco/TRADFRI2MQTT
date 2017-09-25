version="0.1.1"
working_dir=$(shell pwd)

configure:
	python ./scripts/config_env.py prod $(version) all

package-deb-doc:
	@echo "Packing the application into debian package"
	chmod a+x debian/DEBIAN/*
	find . -name ".DS_Store" -type f -delete
	cp target/ikea-ad-$(version)-jar-with-dependencies.jar debian/opt/ikea-ad/ikea-ad.jar
	cp Californium.properties debian/opt/ikea-ad
	find ./debian -name ".DS_Store" -type f -delete
	docker run --rm -v ${working_dir}:/build -w /build --name debuild debian dpkg-deb --build debian
	mv debian.deb ikea-ad_$(version)_all.deb
	@echo "Done"

deb : configure package-deb-doc

.phony : clean