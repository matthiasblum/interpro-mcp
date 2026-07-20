SOURCEDIR   = .
BUILDDIR    = _build

.PHONY: help serve build clean

help:
	sphinx-build --help "$(SOURCEDIR)" "$(BUILDDIR)"

serve:
	sphinx-autobuild -b html --fail-on-warning "$(SOURCEDIR)" "$(BUILDDIR)"

build:
	sphinx-build -b html --fail-on-warning "$(SOURCEDIR)" "$(BUILDDIR)"

clean:
	rm -rf "$(BUILDDIR)"

