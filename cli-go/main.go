package main

import (
	"encoding/json"
	"fmt"
	"io/fs"
	"path/filepath"
	"regexp"
	"strings"
	"time"
)

func main() {
	sourceFileSearcher := NewSourceFileSearcher(
		"/Users/aurambaj/xyz",
		// TODO(jean) include anything without matching? but don't want to use nil.
		// it helps passing a special value does not look proper: nil include means scanning nothing,
		// not everything. Have a specific constant for .+ and check it might be a better option
		[]*regexp.Regexp{regexp.MustCompile(".+")},
		//[]*regexp.Regexp{regexp.MustCompile("\\.idea")},
		nil,
		[]FileType{
			//{
			//	Id:                  "android",
			//	SourceRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)res/values/(?P<basename>strings)\\.(?P<extension>xml)$"),
			//	TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)res/values-(?P<locale>.+?)/(?P<basename>strings)\\.(?P<extension>xml)$"),
			//	TargetTemplate:      "{parentpath}res/values-{locale}/{basename}.{extension}",
			//	DefaultSourceLocale: "en",
			//},
			//{
			//	Id: "angular",
			//	//TODO(jean) this limits the basename to not contain ., might be ok or not...
			//	SourceRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<basename>[^.]+)\\.(?P<extension>xlf)$"),
			//	TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<basename>[^.]+)\\.(?P<locale>[^.]+?)\\.(?P<extension>xlf)$"),
			//	TargetTemplate:      "{parentpath}res/values-{locale}/{basename}.{extension}",
			//	DefaultSourceLocale: "en",
			//},
			//{
			//	Id: "ios-strings",
			//	//TODO(jean) that's where we may want to inject the root locale / or Base / do we want to limit the basename too?
			//	SourceRegexp: regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<locale>en)\\.lproj/(?P<basename>.+)\\.(?P<extension>strings)$"),
			//	//TODO(jean) this will turn up as ambiguous! GO does NOT do backtracking with regex so we're doomed
			//	TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<locale>.+)\\.lproj/(?P<basename>.+)\\.(?P<extension>strings)$"),
			//	TargetTemplate:      "{parentpath}/{locale}.lproj/{basename}.strings",
			//	DefaultSourceLocale: "en",
			//},
			//{
			//	//TODO(jean) or this
			//	Id:                  "ios-strings-base",
			//	SourceRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<locale>Base)\\.lproj/(?P<basename>.+)\\.(?P<extension>strings)$"),
			//	TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<locale>.+)\\.lproj/(?P<basename>.+)\\.(?P<extension>strings)$"),
			//	TargetTemplate:      "{parentpath}/{locale}.lproj/{basename}.strings",
			//	DefaultSourceLocale: "en",
			//},
			{
				Id:                  "po",
				SourceRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)LC_MESSAGES/(?P<basename>.+)\\.(?P<extension>pot)$"),
				TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<locale>.+?)/LC_MESSAGES/(?P<basename>.+)\\.(?P<extension>po)$"),
				TargetTemplate:      "{parentpath}/{locale}/LC_MESSAGES/{basename}.po",
				DefaultSourceLocale: "en",
			},
			//{
			//	Id:                  "properties-optimus",
			//	SourceRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<basename>[^\\_]+)\\.(?P<extension>properties)$"),
			//	TargetRegexp:        regexp.MustCompile("^(?P<parentpath>(?:.+/)?)(?P<basename>[^\\_]+)\\.(?P<locale>[^-]+?)\\.(?P<extension>xlf)$"),
			//	TargetTemplate:      "{parentpath}/{basename}_{locale}.properties",
			//	DefaultSourceLocale: "en",
			//},
			//{
			//	Id:                  "anything",
			//	SourceRegexp:        regexp.MustCompile(".*"),
			//	TargetRegexp:        regexp.MustCompile(""),
			//	TargetTemplate:      "anything",
			//	DefaultSourceLocale: "en",
			//},
		},
		// if not wildcarding test/angular/* the match fails
		//[]string{"test/*/res", "test/angular/"},
		nil,
	)

	start := time.Now()
	mapSourceFiles, _ := sourceFileSearcher.ScanForSourceFiles(map[string]SourceFile{})

	printResult := true
	if printResult {
		fmt.Println("source files:")
		for p, sourceFiles := range mapSourceFiles {
			fmt.Printf("%s: ", p)
			for idx, sf := range sourceFiles {
				prefix := ","
				if idx == 0 {
					prefix = ""
				}
				v, _ := json.Marshal(sf.Fields)
				fmt.Printf("%s%s (%s)", prefix, sf.FileType.Id, v)
			}
			fmt.Println()
		}
	}
	fmt.Printf("source files count: %d\n", len(mapSourceFiles))
	fmt.Printf("elapsed time: %v\n", time.Since(start))
}

type Searcher struct {
	Root string
	// TODO(jean) add subroots. The idea would be to scan from top level directory in the repository
	// but define specific subpath to scan without walking the whole tree
	// before getting there, we should time scanning for a large directory when excluding .git/
	// and all
	// {"src/main/resource", "src/main/comp1/"} (with Root .)
	// could also be regex to say I want to scan all the resource directory {".*/src/main/resource"}
	// and then all the files
	//Subroots      []string
	//SubRootsRegex []*regexp.Regexp
	Dirs         []string
	IncludeRegex []*regexp.Regexp
	ExcludeRegex []*regexp.Regexp
}

func (searcher Searcher) WalkDir(onIncludedFile func(path string) error) (int, error) {
	var numberFileScanned = 0
	return numberFileScanned, filepath.WalkDir(searcher.Root, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		numberFileScanned = numberFileScanned + 1

		// directory pattern that can contains wildcard
		// can represent any directory name
		// dirPattern = "*/src/main"
		// dirPattern = "res/*"
		//
		// when walking the tree, we get path of file and directory
		// we're interested in skipping directory that don't match the pattern but when a
		// while card exist we need to keep scanning because we don't know yet if it will be
		// a match or not
		//
		// filepath = "test/src/main/bla.txt", the paths will be test, test/src/, test/src/main/, test/src/main.bla.txt
		//
		// subroot = "*/*/main"
		// we need to let the directories: test/, test/src/ and test/src/main passthrough
		//
		// we need to reject files in those passthrough directories eg file: test/src/somefile,
		// it is a glob match at this point
		//

		if path != "." && searcher.Dirs != nil {

			if d.IsDir() {
				pathSplit := strings.Split(path, "/")
				globalMatch := false
				for _, dir := range searcher.Dirs {
					//TODO(jean) don't do this all the time, cache it
					dirSplit := strings.Split(dir, "/")
					if dirSplit[len(dirSplit)-1] == "" {
						dirSplit = dirSplit[:len(dirSplit)-1]
					}
					matches := true
					for idx, val := range pathSplit {
						if idx < len(dirSplit) {
							if dirSplit[idx] != "*" && dirSplit[idx] != val {
								matches = false
								break
							}
						} else {
							break
						}
					}
					if matches {
						globalMatch = true
						break
					}
				}

				if !globalMatch {
					return filepath.SkipDir
				}
			} else {
				dirOfPath, _ := filepath.Split(path)
				splitDirOfPath := strings.Split(dirOfPath, "/")

				globalMatch := false
				for _, dir := range searcher.Dirs {
					//TODO(jean) don't do this all the time, cache it
					dirSplit := strings.Split(dir, "/")
					if dirSplit[len(dirSplit)-1] == "" {
						dirSplit = dirSplit[:len(dirSplit)-1]
					}
					matches := true
					if len(dirSplit) > (len(splitDirOfPath) - 1) {
						matches = false
					} else {
						for idx, val := range splitDirOfPath {
							if idx < len(dirSplit) {
								if dirSplit[idx] != "*" && dirSplit[idx] != val {
									matches = false
									break
								}
							} else {
								break
							}
						}
					}
					if matches {
						globalMatch = true
						break
					}
				}

				if !globalMatch {
					return nil
				}

			}
		}

		relativePath, _ := filepath.Rel(searcher.Root, path)
		var ret error = nil

		shouldExclude := searcher.shouldExclude(relativePath)

		if shouldExclude {
			if d.IsDir() {
				ret = filepath.SkipDir
			}
		} else if !d.IsDir() {
			shouldInclude := searcher.shouldInclude(relativePath)

			if shouldInclude {
				ret = onIncludedFile(relativePath)
			}
		}

		return ret
	})
}

func (searcher Searcher) shouldInclude(rel string) bool {
	var included = false
	for _, r := range searcher.IncludeRegex {
		if r != nil && r.MatchString(rel) {
			included = true
			break
		}
	}
	return included
}

func (searcher Searcher) shouldExclude(rel string) bool {
	var excluded = false

	for _, r := range searcher.ExcludeRegex {
		if r != nil && r.MatchString(rel) {
			excluded = true
			break
		}
	}
	return excluded
}

type FileType struct {
	Id           string
	SourceRegexp *regexp.Regexp
	// use this to detect ambiguous file, else no need
	TargetRegexp   *regexp.Regexp
	TargetTemplate string
	//TODO(jean) this typically would be per file not so much per file type eventhough
	// you can imagine a project having a single Locale
	DefaultSourceLocale string
}

type SourceFileSearcher struct {
	searcher  Searcher
	fileTypes []FileType
}

func NewSourceFileSearcher(
	SourceRoot string,
	IncludeRegex []*regexp.Regexp,
	ExcludeRegex []*regexp.Regexp,
	FileTypes []FileType,
	Dir []string,
) *SourceFileSearcher {
	return &SourceFileSearcher{
		searcher: Searcher{
			Root:         SourceRoot,
			IncludeRegex: IncludeRegex,
			ExcludeRegex: ExcludeRegex,
			Dirs:         Dir},
		fileTypes: FileTypes,
	}
}

type SourceFile struct {
	// must have
	SourcePath string
	Fields     OptionalPathFields
	FileType   FileType

	// should have
	SourceRoot string

	// could have
	//targetRoot   string

	// yes but this needs to come from somewhere else and not scanning
	//assetMapping string

	// extracted
}

func (searcher SourceFileSearcher) ScanForSourceFiles(alreadyProcessedFiles map[string]SourceFile) (map[string][]SourceFile, error) {
	var mapSourceFiles = make(map[string][]SourceFile)
	numFileScanned, err := searcher.searcher.WalkDir(func(path string) error {

		// TODO(jean) that assume all the information is already filed up. What if we have
		// partial info and want it to be completed. Should this be done here, or outside
		// outside seem more likely ... unless everything because generic and processed here
		file, ok := alreadyProcessedFiles[path]
		if ok {
			// TODO(jean) this could be skipped and the outer job have this logic
			mapSourceFiles[path] = append(mapSourceFiles[path], file)
		} else {
			for _, fileType := range searcher.fileTypes {
				fields := getFieldsInPath(path, fileType.SourceRegexp)

				if fields != nil {
					if fields.Locale == nil {
						fields.Locale = &fileType.DefaultSourceLocale
					}

					file := SourceFile{
						SourcePath: path,
						FileType:   fileType,
						SourceRoot: searcher.searcher.Root,
						Fields:     *fields,
					}

					mapSourceFiles[path] = append(mapSourceFiles[path], file)
				}
			}
		}
		return nil
	})

	fmt.Printf("Number file scanned: %d\n", numFileScanned)
	return mapSourceFiles, err
}

type OptionalPathFields struct {
	Locale   *string
	Basename *string
	//filename   *string
	Extension  *string
	ParentPath *string
}

func (f OptionalPathFields) String() string {
	return fmt.Sprintf("Locale: %s, Basename: %s, Extension: %s, ParentPath: %s", *f.Locale, *f.Basename, *f.Extension, *f.ParentPath)
}

func getFieldsInPath(path string, re *regexp.Regexp) *OptionalPathFields {
	submatch := re.FindStringSubmatch(path)
	var fields *OptionalPathFields
	if submatch != nil {
		fields = &OptionalPathFields{
			Locale:   fieldOrNil(re, submatch, "locale"),
			Basename: fieldOrNil(re, submatch, "basename"),
			//filename:   fieldOrNil(re, submatch, "filename"),
			Extension:  fieldOrNil(re, submatch, "extension"),
			ParentPath: fieldOrNil(re, submatch, "parentpath")}
	}
	return fields
}

func fieldOrNil(re *regexp.Regexp, match []string, field string) *string {
	idx := re.SubexpIndex(field)
	if idx != -1 {
		return &match[idx]
	}
	return nil
}
