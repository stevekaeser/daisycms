/*
_____________________________________________________________________________

                       PCRE Functions Header v1.0
_____________________________________________________________________________

An NSIS plugin providing Perl compatible regular expression functions.

A simple wrapper around the excellent PCRE library which was written by
Philip Hazel, University of Cambridge.

For those that require documentation on how to construct regular expressions,
please see http://www.pcre.org/

_____________________________________________________________________________

Copyright (c) 2007 Computerway Business Solutions Ltd.
Copyright (c) 2005 Google Inc.
Copyright (c) 1997-2006 University of Cambridge

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

    * Neither the name of the University of Cambridge nor the name of Google
      Inc. nor the name of Computerway Business Solutions Ltd. nor the names
      of their contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

Core PCRE Library Written by:       Philip Hazel, University of Cambridge
C++ Wrapper functions by:           Sanjay Ghemawat, Google Inc.
Support for PCRE_XXX modifiers by:  Giuseppe Maxia
NSIS integration by:                Rob Stocks, Computerway Business Solutions Ltd.

_____________________________________________________________________________

*/

!ifndef PCRELIB_INCLUDED
!define PCRELIB_INCLUDED

!define _PCRELIB_UN

!include LogicLib.nsh

# Macros

!macro RECheckPatternCall RESULT PATTERN
        Push `${PATTERN}`
        Call RECheckPattern
        Pop ${RESULT}
!macroend

!macro un.RECheckPatternCall RESULT PATTERN
        Push `${PATTERN}`
        Call un.RECheckPattern
        Pop ${RESULT}
!macroend

!macro REQuoteMetaCall RESULT SUBJECT
        Push `${SUBJECT}`
        Call REQuoteMeta
        Pop ${RESULT}
!macroend

!macro un.REQuoteMetaCall RESULT SUBJECT
        Push `${SUBJECT}`
        Call un.REQuoteMeta
        Pop ${RESULT}
!macroend

!macro REClearAllOptionsCall
        Call REClearAllOptions
!macroend

!macro un.REClearAllOptionsCall
        Call un.REClearAllOptions
!macroend

!macro REClearOptionCall OPTION
        Push `${OPTION}`
        Call REClearOption
!macroend

!macro un.REClearOptionCall OPTION
        Push `${OPTION}`
        Call un.REClearOption
!macroend

!macro RESetOptionCall OPTION
        Push `${OPTION}`
        Call RESetOption
!macroend

!macro un.RESetOptionCall OPTION
        Push `${OPTION}`
        Call un.RESetOption
!macroend

!macro REGetOptionCall RESULT OPTION
        Push `${OPTION}`
        Call REGetOption
        Pop ${RESULT}
!macroend

!macro un.REGetOptionCall RESULT OPTION
        Push `${OPTION}`
        Call un.REGetOption
        Pop ${RESULT}
!macroend

!macro REMatchesCall RESULT PATTERN SUBJECT PARTIAL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${PARTIAL}`
        Push "0"
        Call REMatches
        Pop ${RESULT}
!macroend

!macro un.REMatchesCall RESULT PATTERN SUBJECT PARTIAL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${PARTIAL}`
        Push "0"
        Call un.REMatches
        Pop ${RESULT}
!macroend

!macro RECaptureMatchesCall RESULT PATTERN SUBJECT PARTIAL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${PARTIAL}`
        Push "1"
        Call REMatches
        Pop ${RESULT}
!macroend

!macro un.RECaptureMatchesCall RESULT PATTERN SUBJECT PARTIAL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${PARTIAL}`
        Push "1"
        Call un.REMatches
        Pop ${RESULT}
!macroend

!macro REFindCall RESULT PATTERN SUBJECT
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Call REFind
        Pop ${RESULT}
!macroend

!macro un.REFindCall RESULT PATTERN SUBJECT
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Call un.REFind
        Pop ${RESULT}
!macroend

!macro REFindNextCall RESULT
        Call REFindNext
        Pop ${RESULT}
!macroend

!macro un.REFindNextCall RESULT
        Call un.REFindNext
        Pop ${RESULT}
!macroend

!macro REFindCloseCall
        Call REFindClose
!macroend

!macro un.REFindCloseCall
        Call un.REFindClose
!macroend

!macro REReplaceCall RESULT PATTERN SUBJECT REPLACEMENT REPLACEALL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${REPLACEMENT}`
        Push `${REPLACEALL}`
        Call REReplace
        Pop ${RESULT}
!macroend

!macro un.REReplaceCall RESULT PATTERN SUBJECT REPLACEMENT REPLACEALL
        Push `${PATTERN}`
        Push `${SUBJECT}`
        Push `${REPLACEMENT}`
        Push `${REPLACEALL}`
        Call un.REReplace
        Pop ${RESULT}
!macroend

# Functions

!macro RECheckPattern
        !ifndef ${_PCRELIB_UN}RECheckPattern
                !define ${_PCRELIB_UN}RECheckPattern `!insertmacro ${_PCRELIB_UN}RECheckPatternCall`
                Function ${_PCRELIB_UN}RECheckPattern
                
                        Exch $0

                        NSISpcre::RECheckPattern /NOUNLOAD $0
                        
                        Pop $0
                        
                        Exch $0

                FunctionEnd
        !endif
!macroend

!macro REQuoteMeta
        !ifndef ${_PCRELIB_UN}REQuoteMeta
                !define ${_PCRELIB_UN}REQuoteMeta `!insertmacro ${_PCRELIB_UN}REQuoteMetaCall`
                Function ${_PCRELIB_UN}REQuoteMeta

                        Exch $0

                        NSISpcre::REQuoteMeta /NOUNLOAD $0

                        Pop $0

                        Exch $0

                FunctionEnd
        !endif
!macroend

!macro REClearAllOptions
        !ifndef ${_PCRELIB_UN}REClearAllOptions
                !define ${_PCRELIB_UN}REClearAllOptions `!insertmacro ${_PCRELIB_UN}REClearAllOptionsCall`
                Function ${_PCRELIB_UN}REClearAllOptions

                        NSISpcre::REClearAllOptions /NOUNLOAD

                FunctionEnd
        !endif
!macroend

!macro REClearOption
        !ifndef ${_PCRELIB_UN}REClearOption
                !define ${_PCRELIB_UN}REClearOption `!insertmacro ${_PCRELIB_UN}REClearOptionCall`
                Function ${_PCRELIB_UN}REClearOption

                        # [OPTION]
                        Exch $0

                        NSISpcre::REClearOption /NOUNLOAD $0

                        Pop $0

                FunctionEnd
        !endif
!macroend

!macro RESetOption
        !ifndef ${_PCRELIB_UN}RESetOption
                !define ${_PCRELIB_UN}RESetOption `!insertmacro ${_PCRELIB_UN}RESetOptionCall`
                Function ${_PCRELIB_UN}RESetOption
                
                        # [OPTION]
                        Exch $0
                        
                        NSISpcre::RESetOption /NOUNLOAD $0
                        
                        Pop $0
                
                FunctionEnd
        !endif
!macroend

!macro REGetOption
        !ifndef ${_PCRELIB_UN}REGetOption
                !define ${_PCRELIB_UN}REGetOption `!insertmacro ${_PCRELIB_UN}REGetOptionCall`
                Function ${_PCRELIB_UN}REGetOption
                
                        # [OPTION]
                        Exch $0

                        NSISpcre::REGetOption /NOUNLOAD $0
                        
                        Pop $0

                        Exch $0 # [RESULT]

                FunctionEnd
        !endif
!macroend

!macro REMatches
        !ifndef ${_PCRELIB_UN}REMatches
                !define ${_PCRELIB_UN}REMatches `!insertmacro ${_PCRELIB_UN}REMatchesCall`
                !define ${_PCRELIB_UN}RECaptureMatches `!insertmacro ${_PCRELIB_UN}RECaptureMatchesCall`
                Function ${_PCRELIB_UN}REMatches
                
                        # [PATTERN, SUBJECT, PARTIAL, CAPTURE]
                        Exch $0 # [PATTERN, SUBJECT, PARTIAL, $0]
                        Exch 3  # [$0, SUBJECT, PARTIAL, PATTERN]
                        Exch $1 # [$0, SUBJECT, PARTIAL, $1]
                        Exch 2  # [$0, $1, PARTIAL, SUBJECT]
                        Exch $2 # [$0, $1, PARTIAL, $2]
                        Exch    # [$0, $1, $2, PARTIAL]
                        Exch $3 # [$0, $1, $2, $3]
                        Push $4
                        
                        ${If} $0 != 0
                                StrCpy $4 5     # Push captured strings under the 5 items at the top of the stack
                        ${Else}
                                StrCpy $4 0     # Push captured strings to the top of the stack
                        ${EndIf}

                        NSISpcre::REMatches /NOUNLOAD $1 $2 $3 $4
                        Pop $1  # true, false or error
                        ClearErrors
                        ${If} $1 == "true"
                                Pop $1  # Number of captured patterns
                                ${If} $0 != 0
                                        # Capturing so leave captured strings on stack
                                        # Returned value is number of captured strings
                                ${Else}
                                        # Remove captured strings from the stack
                                        # Returned value is 'true'
                                        ${For} $2 1 $1
                                                Pop $3
                                        ${Next}
                                        StrCpy $1 "true"
                                ${EndIf}
                        ${ElseIf} $1 == "false"
                                # Do nothing - just return 'false'
                        ${Else}
                                SetErrors
                        ${EndIf}
                        
                        StrCpy $0 $1
                        
                        Pop $4
                        Pop $3
                        Pop $2
                        Pop $1
                        Exch $0
                        
                FunctionEnd
        !endif
!macroend

!macro REFind
        !ifndef ${_PCRELIB_UN}REFind
                !define ${_PCRELIB_UN}REFind `!insertmacro ${_PCRELIB_UN}REFindCall`
                Function ${_PCRELIB_UN}REFind

                        # [PATTERN, SUBJECT]
                        Exch $0 # [PATTERN, $0]
                        Exch    # [$0, PATTERN]
                        Exch $1 # [$0, $1]

                        NSISpcre::REFind /NOUNLOAD $1 $0 2
                        Pop $0  # true, false or error
                        ClearErrors
                        ${If} $0 == "true"
                                Pop $0  # Number of captured patterns
                                # Leave captured strings on stack
                                # Returned value is number of captured strings
                        ${ElseIf} $0 == "false"
                                # Do nothing - just return 'false'
                        ${Else}
                                SetErrors
                        ${EndIf}

                        Pop $1
                        Exch $0

                FunctionEnd
        !endif
        !ifndef ${_PCRELIB_UN}REFindClose
                !define ${_PCRELIB_UN}REFindClose `!insertmacro ${_PCRELIB_UN}REFindCloseCall`
                Function ${_PCRELIB_UN}REFindClose

                        NSISpcre::REFindClose /NOUNLOAD

                FunctionEnd
        !endif
!macroend

!macro REFindNext
        !ifndef ${_PCRELIB_UN}REFindNext
                !define ${_PCRELIB_UN}REFindNext `!insertmacro ${_PCRELIB_UN}REFindNextCall`
                Function ${_PCRELIB_UN}REFindNext
                
                        Push $0

                        NSISpcre::REFindNext /NOUNLOAD 1
                        Pop $0  # true, false or error
                        ClearErrors
                        ${If} $0 == "true"
                                Pop $0  # Number of captured patterns
                                # Leave captured strings on stack
                                # Returned value is number of captured strings
                        ${ElseIf} $0 == "false"
                                # Do nothing - just return 'false'
                        ${Else}
                                SetErrors
                        ${EndIf}

                        Exch $0

                FunctionEnd
        !endif
!macroend

!macro REReplace
        !ifndef ${_PCRELIB_UN}REReplace
                !define ${_PCRELIB_UN}REReplace `!insertmacro ${_PCRELIB_UN}REReplaceCall`
                Function ${_PCRELIB_UN}REReplace

                        # [PATTERN, SUBJECT, REPLACEMENT, REPLACEALL]
                        Exch $0 # [PATTERN, SUBJECT, REPLACEMENT, $0]
                        Exch 3  # [$0, SUBJECT, REPLACEMENT, PATTERN]
                        Exch $1 # [$0, SUBJECT, REPLACEMENT, $1]
                        Exch 2  # [$0, $1, REPLACEMENT, SUBJECT]
                        Exch $2 # [$0, $1, REPLACEMENT, $2]
                        Exch    # [$0, $1, $2, REPLACEMENT]
                        Exch $3 # [$0, $1, $2, $3]

                        NSISpcre::REReplace /NOUNLOAD $1 $2 $3 $0
                        Pop $1  # true, false or error
                        ClearErrors
                        ${If} $1 == "true"
                                Pop $0  # String with substitutions
                        ${ElseIf} $1 == "false"
                                StrCpy $0 ""
                        ${Else}
                                SetErrors
                                StrCpy $0 $1
                        ${EndIf}

                        Pop $3
                        Pop $2
                        Pop $1
                        Exch $0

                FunctionEnd
        !endif
!macroend

# LogicLib support (add =~ and !~ operators to LogicLib)
!macro _=~ _a _b _t _f
  !define _t=${_t}
  !ifdef _t=                                            ; If no true label then make one
    !define __t _LogicLib_Label_${__LINE__}
  !else
    !define __t ${_t}
  !endif

  Push $0
  ${REMatches} $0 ${_b} ${_a} 1
  StrCmp $0 "true" +1 +3
  Pop $0
  Goto ${__t}

  Pop $0
  !define _f=${_f}
  !ifndef _f=                                           ; If a false label then go there
    Goto ${_f}
  !endif
  !undef _f=${_f}

  !ifdef _t=                                            ; If we made our own true label then place it
    ${__t}:
  !endif
  !undef __t
  !undef _t=${_t}
!macroend

!macro _!~ _a _b _t _f
  !define _t=${_t}
  !ifdef _t=                                            ; If no true label then make one
    !define __t _LogicLib_Label_${__LINE__}
  !else
    !define __t ${_t}
  !endif

  Push $0
  !ifdef PCRELLUN
  ${un.REMatches} $0 ${_b} ${_a} 1
  !else
  ${REMatches} $0 ${_b} ${_a} 1
  !endif
  StrCmp $0 "true" +3 +1
  Pop $0
  Goto ${__t}

  Pop $0
  !define _f=${_f}
  !ifndef _f=                                           ; If a false label then go there
    Goto ${_f}
  !endif
  !undef _f=${_f}

  !ifdef _t=                                            ; If we made our own true label then place it
    ${__t}:
  !endif
  !undef __t
  !undef _t=${_t}
!macroend

# Uninstaller support

!macro un.RECheckPattern
	!ifndef un.RECheckPattern
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro RECheckPattern

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REQuoteMeta
	!ifndef un.REQuoteMeta
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REQuoteMeta

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REClearAllOptions
	!ifndef un.REClearAllOptions
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REClearAllOptions

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REClearOption
	!ifndef un.REClearOption
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REClearOption

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.RESetOption
	!ifndef un.RESetOption
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro RESetOption

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REGetOption
	!ifndef un.REGetOption
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REGetOption

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REMatches
	!ifndef un.REMatches
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REMatches

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.RECaptureMatches
	!ifndef un.RECaptureMatches
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro RECaptureMatches

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REFind
	!ifndef un.REFind
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REFind

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REFindNext
	!ifndef un.REFindNext
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REFindNext

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REFindClose
	!ifndef un.REFindClose
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REFindClose

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro un.REReplace
	!ifndef un.REReplace
		!undef _PCRELIB_UN
		!define _PCRELIB_UN `un.`

		!insertmacro REReplace

		!undef _PCRELIB_UN
		!define _PCRELIB_UN
	!endif
!macroend

!macro _un.=~ _a _b _t _f
        !define PCRELLUN
        !insertmacro _=~ `${_a}` `${_b}` `${_t}` `${_f}`
        !undef PCRELLUN
!macroend

!macro _un.!~ _a _b _t _f
        !define PCRELLUN
        !insertmacro _!~ `${_a}` `${_b}` `${_t}` `${_f}`
        !undef PCRELLUN
!macroend

!endif

