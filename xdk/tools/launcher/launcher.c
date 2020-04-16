#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <errno.h>
#include "launcher.h"
#include "os_specific.h"


static void init();
static void freeAll();
static const char* findValue(const char* config, const char* name, const char* defaultValue);
static const char* skipWhitespace(const char* s);
static const char* skipToNextLine(const char* s);
static const char* trimToEOL(const char* s);
static const char* withExtension(const char* file, const char* ext);


/**
 * This program launches Java to handle the command represented by the name of this executable.
 *
 * Inspired by alexkasko's "jar-launcher" GitHub project, and by the "Launch4j" SourceForge project.
 */
int main(int argc, const char* argv[])
    {
    init();

    // first, determine the path to this executable; it is required both to find the location of the
    // JAR and optional config file, and also to figure out the "command name" for this executable
    const char* execPath = findLauncherPath();

    // next, load the optional config for the executable
    const char* execDir  = extractDir(execPath);
    const char* execFile = removeExtension(extractFile(execPath));
    const char* cfgPath  = buildPath(execDir, withExtension(execFile, ".cfg"));
    const char* cfg      = readFile(cfgPath);
    const char* javaPath = findValue(cfg, "exec", DEFAULT_EXEC);
    const char* javaOpts = findValue(cfg, "opts", DEFAULT_OPTS);
    const char* javaJar  = findValue(cfg, "jar" , DEFAULT_JAR );

    // finally, launch the JVM
    execJava(javaPath, javaOpts, javaJar, argc, argv);

    freeAll();
    return 0;
    }

static void* garbage[MAX_GARBAGE];
static int   garbageCount = 0;

/**
 * Initialize data structures.
 */
static void init()
    {
    memset(garbage, 0, MAX_GARBAGE * sizeof(void*));
    }

char* allocBuffer(int size)
    {
    char* buffer = (char*) malloc(size+1);
    memset(buffer, '\0', size+1);
    registerGarbage(buffer);
    return buffer;
    }

void registerGarbage(void* buffer)
    {
    if (garbageCount + 1 >= MAX_GARBAGE)
        {
        free(buffer);
        abortLaunch("garbage overflow");
        }

    garbage[garbageCount++] = buffer;
    }

/**
 * Free all previously registered buffers.
 */
static void freeAll()
    {
    for (int i = 0; i < garbageCount; ++i)
        {
        free(garbage[i]);
        }
    memset(garbage, 0, MAX_GARBAGE * sizeof(void*));
    }

void abortLaunch(const char* message)
    {
    freeAll();

    if (errno)
        {
        perror(message);
        }
    else if (message == NULL)
        {
        printf("Unknown error; aborting.\n");
        }
    else
        {
        printf("Error: %s; aborting.\n", message);
        }

    exit(1);
    }

/**
 * Find the value of a key/value pair in the specified config file contents.
 *
 * Example config file format:
 *
 *      item = whatever
 *      count = 3
 *
 * @param config        the config file contents (or NULL)
 * @param name          the name to search for in the config
 * @param defaultValue  the default value to return if the name is not found in the config
 *
 * @return the value to use
 */
static const char * findValue(const char* config, const char* name, const char* defaultValue)
    {
    if (config == NULL)
        {
        return defaultValue;
        }

    int         lenName = strlen(name);
    const char* cur     = skipWhitespace(config);
    while (*cur != '\0')
        {
        if (strncmp(cur, name, lenName) == 0)
            {
            cur = skipWhitespace(cur + lenName);
            if (*cur == '=')
                {
                return trimToEOL(cur+1);
                }
            }
        cur = skipToNextLine(cur);
        }

    return defaultValue;
    }

static const char* skipWhitespace(const char* s)
    {
    for (;;)
        {
        switch (*s)
            {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                ++s;
                break;

            default:
                return s;
            }
        }
    }

static const char* skipToNextLine(const char* s)
    {
    for (;;)
        {
        switch (*s)
            {
            case '\0':
                return s;

            case '\n':
                return s+1;

            default:
                ++s;
            }
        }
    }

static const char* trimToEOL(const char* s)
    {
    const char* first = skipWhitespace(s);
    const char* last  = first-1;
    const char* cur   = first;
    for (;;)
        {
        switch (*cur)
            {
            case '\0':
            case '\n':
                {
                int len = last - first + 1;
                if (len <= 0)
                    {
                    return "";
                    }

                char* result = allocBuffer(len);
                strncpy(result, first, len);
                return result;
                }

            case ' ':
            case '\t':
            case '\r':
                cur++;
                break;

            default:
                last = cur++;
                break;
            }
        }
    }

const char* removeExtension(const char* file)
    {
    char* dot = strchr(file, '.');
    if (dot == NULL)
        {
        return file;
        }

    int   len    = dot - file;
    char* result = allocBuffer(len);
    memcpy(result, file, len);
    return result;
    }

static const char* withExtension(const char* file, const char* ext)
    {
    if (*ext == '.')
        {
        ++ext;
        }

    char* dot     = strchr(file, '.');
    int   fileLen = dot == NULL ? strlen(file) : (dot - file);
    int   extLen  = strlen(ext);
    int   total   = fileLen + 1 + extLen;
    char* result  = allocBuffer(total);
    memcpy(result, file, fileLen);
    result[fileLen] = '.';
    memcpy(result + fileLen + 1, ext, extLen);
    return result;
    }

const char** toArgV(const char* cmd, int* argc)
    {
    // deal with no command string, or empty command string; translate both of these to zero args
    if (cmd == NULL || *(cmd = skipWhitespace(cmd)) == '\0')
        {
        const char** argv = malloc(sizeof(const char*));
        argv[0] = NULL;
        registerGarbage(argv);

        if (argc != NULL)
            {
            *argc = 0;
            }

        return argv;
        }

    int count = 0;
    if (*cmd != '\0')
        {
        const char* cur = cmd;
        const char* space;
        while ((space = strchr(cur, ' ')) != NULL)
            {
            ++count;
            cur = skipWhitespace(space);
            }
        ++count;
        }

    const char** argv = malloc((count+1) * sizeof(const char*));
    argv[count] = NULL;
    registerGarbage(argv);

    const char* cur = cmd;
    for (int i = 0; i < count; ++i)
        {
        const char* space = strchr(cur, ' ');
        if (space == NULL)
            {
            assert(i == count-1);
            argv[i] = cur;
            }
        else
            {
            int   len = space - cur;
            char* arg = allocBuffer(len);
            memcpy(arg, cur, len);
            argv[i] = arg;
            cur     = skipWhitespace(space);
            }
        }

    if (argc != NULL)
        {
        *argc = count;
        }

    return argv;
    }

